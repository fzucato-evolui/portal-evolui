[console]::InputEncoding = [console]::OutputEncoding = New-Object System.Text.UTF8Encoding
java -jar "-Dfile.encoding=UTF-8" health-checker.jar