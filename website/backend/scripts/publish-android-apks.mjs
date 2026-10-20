#!/usr/bin/env node
// Validate and publish separately signed T53 Child and Parent APKs.
// Usage: node scripts/publish-android-apks.mjs /path/to/child-release.apk /path/to/parent-release.apk 1.0.0
import { createHash } from "node:crypto";
import { copyFileSync, existsSync, mkdirSync, readFileSync, readdirSync, statSync, writeFileSync } from "node:fs";
import { dirname, join, resolve, basename } from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const ORIGIN = process.env.T53_PUBLIC_ORIGIN || "https://t53.dynamicrobotics53.com";
const MAX_BYTES = 25 * 1024 * 1024;
const expected = { child: "com.t53.telecom53.child", parent: "com.t53.telecom53.parent" };
function fail(msg) { console.error("ERROR: " + msg); process.exit(1); }

function sdkTool(name) {
  const roots = [process.env.ANDROID_SDK_ROOT, process.env.ANDROID_HOME, join(process.env.HOME || "", "Library/Android/sdk"), join(process.env.HOME || "", "Android/Sdk")].filter(Boolean);
  for (const root of roots) {
    const bt = join(root, "build-tools");
    if (!existsSync(bt)) continue;
    const versions = readdirSync(bt).filter(v => existsSync(join(bt, v, name))).sort((a,b)=>b.localeCompare(a, undefined, {numeric:true}));
    if (versions.length) return join(bt, versions[0], name);
  }
  return null;
}
function run(tool, args) {
  const r = spawnSync(tool, args, { encoding: "utf8" });
  if (r.error || r.status !== 0) fail(`Tool failed: ${tool} ${args.join(" ")}\n${r.stderr || r.error || "unknown error"}`);
  return (r.stdout || "") + (r.stderr || "");
}
function inspect(role, filename, expectedPackage) {
  const p = resolve(filename);
  if (!existsSync(p) || !statSync(p).isFile()) fail(`${role}: file not found: ${p}`);
  if (!/\.apk$/i.test(p)) fail(`${role}: expected an .apk file`);
  if (/debug|unsigned/i.test(basename(p))) fail(`${role}: filename suggests a debug/unsigned build: ${basename(p)}`);
  const data = readFileSync(p);
  if (data.length < 4 || data[0] !== 0x50 || data[1] !== 0x4b || !data.includes(Buffer.from("AndroidManifest.xml"))) fail(`${role}: not a recognizable APK`);
  if (data.length > MAX_BYTES) fail(`${role}: ${(data.length/1048576).toFixed(1)} MiB exceeds the 25 MiB per-file hosting limit`);
  const apksigner = process.env.APKSIGNER || sdkTool("apksigner") || "apksigner";
  const sig = run(apksigner, ["verify", "--verbose", "--print-certs", p]);
  if (/CN=Android Debug/i.test(sig)) fail(`${role}: signed with the Android DEBUG certificate. Sign with your own release keystore.`);
  if (!/Verified using v2 scheme[^\n]*true|Verified using v3 scheme[^\n]*true/i.test(sig)) fail(`${role}: no v2/v3 APK signature found.`);
  const aapt = process.env.AAPT || sdkTool("aapt2") || sdkTool("aapt");
  if (!aapt) fail("Android build-tools 'aapt2' was not found. Set ANDROID_SDK_ROOT or AAPT before publishing.");
  const badging = run(aapt, ["dump", "badging", p]);
  if (/application-debuggable/.test(badging)) fail(`${role}: APK is marked debuggable. Build the release variant.`);
  const match = badging.match(/^package: name='([^']+)'/m);
  if (!match || match[1] !== expectedPackage) fail(`${role}: expected package ${expectedPackage}, got ${match ? match[1] : "unknown"}`);
  const digest = createHash("sha256").update(data);
  return { path:p, data, sizeBytes:data.length, sha256:digest.copy().digest("hex"), checksumB64Url:digest.digest("base64").replace(/\+/g,"-").replace(/\//g,"_") };
}

const [, , childArg, parentArg, version] = process.argv;
if (!childArg || !parentArg || !version) fail("Usage: node scripts/publish-android-apks.mjs /path/to/child-release.apk /path/to/parent-release.apk 1.0.0");
if (!/^[0-9A-Za-z._-]+$/.test(version)) fail("Version may contain only letters, numbers, dot, dash, underscore.");
let QR; try { QR = (await import("qrcode")).default; } catch (e) { fail("Package qrcode is missing. Run npm install in backend before publishing."); }
const child = inspect("T53 Child", childArg, expected.child);
const parent = inspect("T53 Parent", parentArg, expected.parent);
const here = dirname(fileURLToPath(import.meta.url));
const out = join(here, "..", "public", "download");
mkdirSync(out, {recursive:true});
const childFile = "T53-Android.apk"; // legacy-compatible child URL
const parentFile = "T53-Parent-Android.apk";
copyFileSync(child.path, join(out, childFile));
copyFileSync(parent.path, join(out, parentFile));
const updated = new Date().toISOString();
const meta = (f, v, minAndroid) => ({file:f, version, sizeBytes:v.sizeBytes, sha256:v.sha256, minAndroid});
const manifest = {version, updated, child:meta(childFile,child,"9"), parent:meta(parentFile,parent,"8")};
writeFileSync(join(out,"apks.json"), JSON.stringify(manifest,null,2)+"\n");
writeFileSync(join(out,"apk.json"), JSON.stringify({file:childFile,version,sizeBytes:child.sizeBytes,sha256:child.sha256,updated},null,2)+"\n");

const payload = {
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.t53.telecom53.child/com.t53.telecom53.T53DeviceAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": `${ORIGIN}/download/${childFile}`,
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": child.checksumB64Url,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true
};
writeFileSync(join(out,"provision.json"),JSON.stringify(payload,null,2)+"\n");
const svg=await QR.toString(JSON.stringify(payload),{type:"svg",errorCorrectionLevel:"L",margin:3,color:{dark:"#000000",light:"#ffffff"}});
writeFileSync(join(out,"provision-qr.svg"),svg);
console.log(`Validated and published T53 Child ${version}: ${(child.sizeBytes/1048576).toFixed(2)} MiB; SHA-256 ${child.sha256}`);
console.log(`Validated and published T53 Parent ${version}: ${(parent.sizeBytes/1048576).toFixed(2)} MiB; SHA-256 ${parent.sha256}`);
console.log("Child QR generated. Deploy the backend/site, then verify both download buttons and scan the QR on a fresh test device.");
