# 抢券压测启动脚本（非 GUI 模式）
# 用法示例：
#   .\run-jmeter.ps1 -CouponId 4018 -SeckillThreads 11 -LoginThreads 50
param(
    [string]$JMeterHome = 'D:\jmeter\apache-jmeter-5.6.3',
    [int]$CouponId = 4,
    [int]$LoginThreads = 100,
    [int]$LoginLoops = 2,
    [int]$SeckillThreads = 11,
    [int]$SeckillLoops = 1,
    [int]$Ramp = 2
)

$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:\Users\27253\.jdks\ms-17.0.18'
# jmeter.bat 依赖 findstr 等系统命令，确保 PATH 含 System32
if ($env:PATH -notlike '*Windows\System32*') {
    $env:PATH = "C:\Windows\System32;C:\Windows;$env:PATH"
}

$plan = Join-Path $PSScriptRoot 'seckill.jmx'
$result = Join-Path $PSScriptRoot 'result.jtl'
$report = Join-Path $PSScriptRoot 'report'

if (-not (Test-Path $plan)) { throw "找不到压测计划：$plan" }
Remove-Item $result -Force -ErrorAction SilentlyContinue
Remove-Item $report -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "开始压测：登录 ${LoginThreads} 线程 × ${LoginLoops} 次；抢券 ${SeckillThreads} 线程 × ${SeckillLoops} 次；券 ID = $CouponId"
& (Join-Path $JMeterHome 'bin\jmeter.bat') -n -t $plan -l $result -e -o $report `
    "-JcouponId=$CouponId" `
    "-JloginThreads=$LoginThreads" "-JloginLoops=$LoginLoops" `
    "-JseckillThreads=$SeckillThreads" "-JseckillLoops=$SeckillLoops" `
    "-Jramp=$Ramp"

if ($LASTEXITCODE -ne 0) { throw "JMeter 退出码 $LASTEXITCODE" }

# 汇总统计
$rows = Import-Csv $result
Write-Host "`n===== 汇总（按请求分组）====="
$rows | Group-Object label | ForEach-Object {
    $elapsed = $_.Group | ForEach-Object { [int]$_.elapsed } | Sort-Object
    $count = $elapsed.Count
    $avg = [math]::Round(($elapsed | Measure-Object -Average).Average, 1)
    $p90 = $elapsed[[math]::Min($count - 1, [int]($count * 0.9))]
    $p95 = $elapsed[[math]::Min($count - 1, [int]($count * 0.95))]
    [pscustomobject]@{
        请求    = $_.Name
        请求数  = $count
        平均ms  = $avg
        P90     = $p90
        P95     = $p95
        最小    = $elapsed[0]
        最大    = $elapsed[-1]
        错误率  = ('{0:P1}' -f (($_.Group | Where-Object { $_.success -eq 'false' }).Count / $count))
    }
} | Format-Table -AutoSize

$total = $rows.Count
$span = ([datetime]'1970-01-01').AddMilliseconds([int64]($rows[-1].timeStamp)) - ([datetime]'1970-01-01').AddMilliseconds([int64]($rows[0].timeStamp))
Write-Host ("总请求 {0} 次，耗时 {1:N1} 秒，吞吐 {2:N1} req/s" -f $total, $span.TotalSeconds, ($total / [Math]::Max($span.TotalSeconds, 0.001)))
Write-Host "HTML 报告：$report\index.html"
