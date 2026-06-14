@echo off
echo ========================================
echo Cleaning duplicate package declarations
echo ========================================

cd /d C:\Users\fengm\Desktop\Code\MoonlightBay\src\main\java

:: 处理所有 Java 文件，删除前4行（重复的package）
for /R %%f in (*.java) do (
    echo Processing: %%f
    setlocal enabledelayedexpansion
    set first=1
    (
        for /f "tokens=*" %%a in (%%f) do (
            set line=%%a
            if !first!==1 (
                echo package moonlightbay; > temp_clean.java
                set first=0
            )
            echo !line! | findstr /v "package moonlightbay" >> temp_clean.java
        )
    )
    move /y temp_clean.java %%f >nul
    endlocal
)

echo Done!
pause
