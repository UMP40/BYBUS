# Architecture hardening (2026-09-22)

This change implements the findings in the local architecture audit while retaining Kotlin/Compose, the Java container and the C++ runtime. It does not change the game APK or the translation source.

## Ownership and commit rules

- Runtime translation activation and deactivation own only host index files. Deactivation must never infer ownership of game JSON from a missing marker or from the newest cached release. Legacy file restoration is an explicit, receipt-backed migration operation.
- Pairing uses stable identity whenever available. Duplicate or absent identities never fall back to a different object's array position. Activation requires valid structured resources, a stable input snapshot and a non-collapsed record count; failures preserve the previous generation.
- Import prepares a complete immutable APK snapshot before changing committed metadata. External container installation has a durable repair marker; committed metadata advances only after installation and native library preparation succeed. Native libraries are staged before replacement, with recovery for interrupted replacement. Stop the guest before any import or translation mutation.
- One application-owned coordinator serializes normal actions across Activity recreation. A shared file lock also covers debug actions and other process instances. ViewModels subscribe to this coordinator rather than owning background jobs.
- Protocol parsing uses JSON structure, not field order. HTTP responses always close; API bodies, archive download, extraction memory and elapsed network work have limits. Offline launch uses the last validated persistent index, with the actual error category shown.
- Readiness labels distinguish saved translation files, container initialization and guest runtime confirmation. Unknown native compatibility profiles must not be treated as verified game builds. Do not invent hashes, offsets or successful device results.
- Automatic cloud backup and device transfer exclude container state, credentials and reconstructible artifacts. Cache retention protects active/previous generations and legacy restoration receipts.

## Validation

Convert the audit's destructive-uninstall, partial-import, invalid-JSON, identity-mismatch and HTTP probes to regression tests, exercise task exclusion and recovery states, then run unit tests and debug/release builds. Verify APK alignment/signature and upgrade identity. Device-dependent behavior remains explicitly unverified until the target phone is connected.

## Implemented boundaries (1.5-llc.3 / versionCode 8)

| Audit finding | Change and validation boundary |
| --- | --- |
| F01 resource deletion | Deactivation uses the active index and removes host pointers only. Tests cover a newer pending release and repeated deactivation. Legacy restoration requires a version/archive ownership receipt and validates all original files before restoring any. |
| F02 partial imports | Immutable APK generations, copied-file hashes, an external-install repair marker, staged native directories and commit-after-install metadata. Fault-injection tests cover missing splits and external installation failure across runtime recreation. This is recoverable synchronization, not a cross-process transaction or full VirtualApp rollback. |
| F03 incomplete index activation | Structured valid resource pairs count toward readiness; missing files remain in the denominator. Activation checks 90% file coverage, 90% field pairing, record-count regression and a stable input fingerprint. Failed compilation preserves the prior generation. |
| F04 incorrect identity matching | Stable identity mismatch, missing identity and duplicate identity never fall back to another item's array position. Tests cover reorder, duplicate and unmatched cases. Identity-less equal-size arrays still use position because those resources expose no stronger key. |
| F05 version-bound native operations | A shipped profile must match versionCode, Android SDK range, ABI and both `libcovault-appsec.so` / `libil2cpp.so` hashes. Empty/unknown profiles fail closed. Pure profile-selection tests do not validate actual game compatibility. |
| F06 task ownership | One application coordinator survives Activity disposal; JVM and process file locks cover UI/debug mutations. Lock contention is tested. Android process death still aborts the task; persistent commit/repair markers support the next attempt. |
| F07 JSON protocol parsing | Structured JSON for release metadata and cache manifests; exact asset selection, null digest support and duplicate rejection. Tests use reordered fields and null values. |
| F08 bounded IO | Close all error responses; a shared 90-second network deadline, 2 MiB API bodies, 64 MiB download, 5,000 archive entries, 128 MiB decoder memory, 2 MiB per mapped file, 128 MiB mapped and 256 MiB total expansion. Tests cover failed streams, body/deadline limits, ignored entries and directories. These are network/decode budgets, not an end-to-end mobile launch deadline. |
| F09 truthful readiness | Container initialization failures affect availability. Per-launch native receipts distinguish index loaded and hooks installed. UI never calls these game-screen, font or login confirmation. The target vivo device and microG on 16 KiB remain unverified. |
| F10 ownership and retention | Disable cloud/device-transfer backup; keep current/previous import and index generations plus the incoming translation release. Preserve legacy baselines. Remove only recognized owned obsolete/staging files. Retention tests protect active/previous and legacy data. |
| F11 responsibilities | Separate repository, pure JSON pairing, legacy migration, atomic filesystem helpers, runtime receipts and coordinator. The vendored Java container's public API uses typed calls. Hidden Android compatibility reflection remains in the vendor module. Kotlin/Java/C++ are retained. |
| F12 evidence gaps | Replace three ignored obsolete redirect tests with runnable regressions using the real JVM JSON implementation. Debug/Release native compilation and APK checks are included. There is no native reader fuzz suite or target-device lifecycle/integration test in this change. |

## Compatibility profile status

The initial architecture-only build (1.5-llc.3) shipped `profiles.count=0` because no target-device binaries were available. The current 1.5-llc.4 candidate contains one tightly scoped profile from the user's installed game 471: SDK 36, arm64, 4 KiB pages and both verified library SHA-256 hashes. Its observed calls and validation limits are recorded below. Historical upstream Android 12 observations do not establish compatibility for vivo / Android 16; merely matching hashes does not establish that every native operation or login path works.

The main APK's 16 KiB alignment does not validate embedded microG libraries. The existing verified microG APK bytes and complete Chinese font are preserved. No rewrite into another language would remove Android process isolation, private API, guest lifecycle or binary compatibility constraints.

## Delivery evidence

The fixed output directory contains `修复说明.md`, `验证结果.json` and the signed `LimbusZeroLauncher.apk`. The architecture audit and its reproductions describe versionCode 7 before these changes and remain historical evidence. Current build/test counts, artifact hash, signature, host alignment and device limitations belong to the new verification report. Desktop extraction timing excludes network, index compilation and guest startup.

## vivo diagnostic follow-up

The user's September 22 diagnostic bundle identifies Android 16 / 4 KiB pages, game versionCode 471, and launcher versionCode 8. The launcher process survives. The guest logs AppSealing `50040` before being recorded as `SIGNALED / 9`; no translation index is active. This is evidence of a game/container compatibility failure, not a Chinese resource parsing failure or a 16 KiB alignment failure. A trusted profile cannot be derived from an error number alone.

The same trace shows first font preparation blocking the guest main thread for roughly nine seconds. Move bounded, hash-verified font extraction to the host launch worker before opening the guest. The guest only checks that the prepared file exists; secondary game services must not configure the text runtime or overwrite the main game's launch receipt. These changes address startup latency and receipt ownership, but do not establish that the AppSealing termination is resolved. Native build inspection and a device retest remain required before publishing a release described as fixing that termination.

The connected device reports game 1.114.0 / 471. Its installed arm64 split was inspected locally: both `50040` sites at `0x54488` and `0x545e4` are `blr x5` calls preceded by `w2=50040`; the `70034` path and cleanup targets retain the documented control flow. A test profile is limited to SDK 36, 4 KiB pages, arm64, version 471 and the exact SHA-256 of both native libraries. The obfuscated on-disk `0x4e564` region does not independently establish a safe `50048` target for this build: that additional cleanup patch and SIGILL fallback remain disabled unless runtime evidence qualifies them separately. No game APK or native library is modified on disk or included in the repository/release.

Device observation at 17:24 confirms `50048` on the same thread as the bounded alarm backtrace, returning through `libcovault-appsec.so+0x22c9c` and `+0x4e568`. The live words at `+0x4e558` and `+0x4e564` are `0x52987002` (`mov w2, #50048`) and `0xd63f0260` (`blr x19`). This independently qualifies the report call and the already inspected same-function continuation at `+0x4e998` for this binary. Enable the scoped full profile for retesting, remove the temporary instruction observer, and keep the four immutable patch sites guarded by their statically verified words. The historical Android 13 background-thread workaround is not enabled by this Android 16 profile. End-to-end launch still requires a new device test.

The subsequent test no longer reports `50040`/`50048`, but reaches `30010` after the auxiliary `:ip_service` cannot resolve its native method. The existing proc redirector hardcodes every `/proc/*/cmdline` to the main package name. Export the actual VirtualRuntime process identity before loading guest libraries, and use it only for this process's `self`/numeric-PID cmdline. Other PIDs must retain their actual kernel result instead of inheriting the caller's name. This corrects service initialization identity; it does not add a `30010` report bypass.

The corrected service initializes, but `30010` persists. A bounded runtime observer independently identifies `libil2cpp.so`, both code-section lengths, and caller `+0x70128` / dispatcher `+0x22594`. The original file remains unchanged. The library decrypts its code when loaded. Add an owned-code-change ledger, verify every current patch still equals its recorded after-bytes, and calculate the original scanner's normalized CRC across the complete `.text` and `il2cpp` sections while substituting only recorded before-bytes. Both results must match this binary's independently observed original checksums (`65c447b9` / `f218ebc2`). The dispatcher may return only for that qualified call when the full comparison succeeds; every unknown change or other report is forwarded. No error-policy table is changed. This is a candidate compatibility fix until device validation succeeds.

Device recomputation at 18:26 matches both original CRCs with 66 registered patches. Merely returning from the report still enters the caller's unconditional failure/termination block. For this verified case, resume at `+0x70464`, the same cleanup reached by its successful comparison, after checking the continuation instructions. Unknown cases retain the original return address. Eleven standalone native integrity checks passed on the connected arm64 phone.

The guard refresh previously stopped with the four-second library scan; retain its normal signal-chain wrapper during the existing bounded startup maintainer (roughly every 50 ms), so later handler installation still forwards real faults correctly. Reduce repeated maps-entry logs which obscure native diagnostics on the OEM log service, and close the original proc descriptor if temporary-file creation fails.
# Android 16 JNI 崩溃定位更正（2026-09-23）

先前 `pc_map` 的 `+0x248d4` 是地址相对单个内存映射的偏移，不是 ELF 基址偏移。
用同一进程实测的库基址核对后，v471 的实际位置为 `libcovault-appsec.so+0xd18d4`。
该处通过 `X19` 中的 `JNIEnv*` 读取 FindClass 函数。所属函数入口为 `+0xd1100`，
第二参数进入 X19，静态直接调用点为 `+0x758a8` 和 `+0x93e40`。
当前先在已验证版本内记录入口参数与 VM 的当前线程环境，不改变检查结果；
不得扩大历史 Android 13 线程终止特判，也不得在信号处理器里附加 JNI 线程。

继续追溯发现 `30010` 失败分支在上报之前，于 `+0x700ec` 调用 `+0xac878`，
后者通过 JavaVM 函数表 `+0x28` 调用 DetachCurrentThread。上一候选版本只在
上报时跳回成功分支，因此复用了已被释放的栈内 JNIEnv。将完整 CRC 判定前移到
该特定释放调用：验证 caller、指令、栈记录和 VM 返回的当前线程 JNIEnv 后，
仅在两段完整代码均只含已登记的自身修改时恢复到成功分支。所有其他释放调用
仍执行原函数；移除上报入口恢复执行的旧路径，避免重建或伪造 JNI 指针。

真机确认该修正维持了相同且有效的 JNIEnv，并完成 `PlayDone`。随后 `30009`
来自 `+0x70ba4`，模块是保护库自身，原始校验值 `6430d800`，描述索引 1、179 个
代码范围；它也在上报前释放 JNI。为这一独立路径登记保护库静态补丁，按原始逐字节
CRC 对全部描述范围复核，不能复用 IL2CPP 的校验结论。临时 JNI 观察 hook 随定位完成移除。

新 profile 中不再对保护库扫描到的所有 kill/exit 系统调用改写为成功返回。
已定位的兼容分支在清理之前处理，其他保护失败保留原退出语义；只保留已确认的
`openat +0x18bf8` 文件重定向和明确指令匹配的静态调用点。动态改写的 `50048` 位置
不登记为可还原的静态原字节；若它影响校验结果，该校验必须拒绝。

保护库范围表的完整 179 项 offset/length 指纹实测为 `dd108bf95840fd56`
（固定编码的 FNV-1a 64），仅用作已匹配二进制内的描述表一致性检查，不作为内容安全校验。
内容仍须重算全部范围的 CRC 并匹配原值。新增纯 native 用例覆盖不连续范围、
跨范围补丁、范围间空隙、修改后二次篡改、越界和描述表变化。

收紧 syscall 处理后，真机在 11:46:55 和后续轮次得到完整原始 CRC `6430d800`，
已登记六个静态补丁，其中 16 个字节落入校验范围；两段 IL2CPP 校验同时继续匹配。
标题页和登录方式选择页已可交互，账号登录、资源下载和游戏内汉化显示另行验证。
移除临时探针。SIGSEGV 诊断只在下游未消费同步故障时生成；ART 已更新上下文的
常规隐式检查立即返回，不重复读取 maps 或输出崩溃日志。历史 Android 13 误用段内
偏移的线程退出特判移除，保留真实同步故障的默认终止语义。

## 2026-09-23 实机交付验证

- 用户完成 Google 登录和容器资源下载后，启动器激活零协 `2026092102` 的 schema 8 索引：100,097 条完整映射、22,739 条术语及 161,210 条源记录。
- 候选构建重新启动后保留登录状态，进入主界面；抽查角色列表、人格详情、技能名称和带数值/富文本的技能说明，显示中文且未见缺字方框。图片内烘焙的文字不在运行时文本替换范围内。
- 移除临时原生探针，兼容校验日志按模块/结果各记录一次。Release 覆盖原候选包成功，保留下载资源、登录和索引；冷启动再次进入中文主界面。启动器收到本次索引与文本接管已加载的运行时回执。
- 91 项 JVM 测试（21 组）通过，0 失败、0 跳过。Android arm64 独立完整性用例通过；签名、完整字体/microG 哈希、宿主 ELF/ZIP 16 KiB 对齐及无私钥/临时探针打包检查通过。
- 本机首次索引准备约 14.96 秒，覆盖后的已有缓存启动准备约 2.48 秒。这是两次实际操作的日志时间，包含各自更新检查，不是控制变量性能基准，也不含游戏联网和进入主界面的时间。
- 未执行手机重启、完整战斗/剧情巡检、Apple 登录或其他设备/游戏构建验证；16 KiB 页设备上的内置 microG 仍未验证。
# v1.0 / 内部版本 37：发布检查与缓存恢复（2026-09-30）

本轮处理只读审查发现的问题：资源准备后的首次离线索引构建、更新 Intent 在 Activity
重建时重复打开页面、零协发布查询和限流状态未共享。继续使用同一正式签名，不改
游戏二进制、原生接管、计费返回或已有账号与资源。

- 首页跟踪与启动检查共用应用级 GitHub 发布仓库和限流记录。仅共享已校验的公开发布
  元数据、短时间内的在途/刚完成请求；启动不直接复用十五分钟展示缓存。服务端重试
  时间同时约束两处请求，失败保留上次记录，并通知在场的页面更新状态。
- 离线回退首先完整验证当前汉化包；没有生效索引时，允许选择持久缓存中的完整版本
  构建索引或进入资源准备模式。损坏归档不得激活，未就绪资源仍受原有配对门槛约束。
- 已有归档在下载层与解压层之间减少一次重复哈希读取；完整归档与解压文件校验保留。
- Google 组件全部安装成功后才清理已不再被配置或安装记录引用的旧内置暂存目录；
  升级失败不清理回退文件，外部配置和账号目录不在清理范围内。
- 只在新收到更新 Intent 时打开更新页；Activity 状态恢复不重放已处理的请求。

验证限定为上述边界的定向测试、签名 Release 构建及必要设备检查。没有新发现的
启动链路故障时不反复开启游戏，不运行完整业务套件；实际结果记录如下。

本轮实际结果：29 个定向用例通过（缓存恢复 3、组件引用保护 2、下载与损坏恢复 6、
发布查询/限流/并发 14、既有索引就绪保护 4）。新增边界后仅重跑受影响发布用例并补跑
上述 4 项就绪检查；共 42 次用例执行，未运行完整套件。签名 Release / lintVital、
APK 资产哈希及 ELF/ZIP 16 KiB 对齐通过，原生库、字体与 Google APK 内容沿用 code36。
共 4 次构建、3 次覆盖安装；最后一次按用户要求完全撤回手势跟随缩放，菜单实现
恢复 code36。最终安装包核对左侧返回到设置、右侧返回到首页通过，未重复业务测试。

vivo X100 Ultra / Android 16 上只启动游戏一次，从现有缓存走完准备流程，保留登录和
资源并进入中文主界面。此检查完成于同轮撤回动效之前，启动逻辑与资源随后未变，
最终包没有再启动游戏。没有操作商品或支付，也未重新巡检所有文本。旧暂存清理的
引用保护通过文件系统用例验证，未把此次启动宣称为测得实际回收字节数或启动加速比例。
