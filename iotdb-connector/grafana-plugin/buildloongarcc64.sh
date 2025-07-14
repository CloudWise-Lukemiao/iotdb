#!/bin/bash


rm -rf dist/gpx_apache_iotdb_datasource_linux_loong64*

env CGO_ENABLED=0 GOOS=linux GOARCH=loong64 go build \
  -o "dist/gpx_apache_iotdb_datasource_linux_loong64" \
  -tags "arrow_json_stdlib" \
  -ldflags "-w -s -extldflags \"-static\" -X 'github.com/grafana/grafana-plugin-sdk-go/build.buildInfoJSON={\"time\":1751624588982,\"pluginID\":\"apache-iotdb-datasource\",\"version\":\"1.0.1\"}' -X 'main.pluginID=apache-iotdb-datasource' -X 'main.version=1.0.1'" \
  "./pkg"

echo "LoongArch64 finish: dist/gpx_apache_iotdb_datasource_linux_loong64"
#!/bin/bash

## 删除旧的 macOS ARM64 二进制文件（如果存在）
#rm -rf dist/gpx_apache_iotdb_datasource_darwin_arm64*
#
## 设置 GOOS 和 GOARCH 为 macOS 和 arm64
#env GOOS=darwin GOARCH=arm64 go build \
#  -o "dist/gpx_apache_iotdb_datasource_darwin_arm64" \
#  -tags "arrow_json_stdlib" \
#  -ldflags "-w -s -extldflags \"-static\" -X 'github.com/grafana/grafana-plugin-sdk-go/build.buildInfoJSON={\"time\":1751624588982,\"pluginID\":\"apache-iotdb-datasource\",\"version\":\"1.0.1\"}' -X 'main.pluginID=apache-iotdb-datasource' -X 'main.version=1.0.1'" \
#  "./pkg"
#
#echo "ARM64 (macOS) finish: dist/gpx_apache_iotdb_datasource_darwin_arm64"
