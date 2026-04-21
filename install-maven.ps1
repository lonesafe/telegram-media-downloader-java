$mavenUrl = "https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"
$tempPath = Join-Path $env:TEMP "apache-maven-3.9.9-bin.zip"
Write-Host "下载 Maven 3.9.9..."
Invoke-WebRequest -Uri $mavenUrl -OutFile $tempPath -UseBasicParsing
Write-Host "下载完成，解压中..."
Expand-Archive -Path $tempPath -DestinationPath "C:\Program Files\" -Force
$mavenHome = "C:\Program Files\apache-maven-3.9.9"
Write-Host "Maven 已安装到 $mavenHome"
Write-Host "添加至 PATH..."
[Environment]::SetEnvironmentVariable("PATH", "$mavenHome\bin;$env:PATH", "Machine")
$env:PATH = "$mavenHome\bin;$env:PATH"
Write-Host "验证安装:"
& "$mavenHome\bin\mvn" --version
