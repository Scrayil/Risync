# SYNCTHING LIBRARY COMPILATIONS
```sh
cd /home/$USER/Desktop/dev/git/syncthing
git checkout main
git pull
git tag | tail
git checkout <LATEST_STABLE_TAG>
export NDK=/home/$USER/Android/Sdk/ndk/<VERSION>
export CC=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android<API>-clang
CGO_ENABLED=1 GOOS=android GOARCH=arm64 go build -tags noupgrade -trimpath -ldflags="-checklinkname=0" -o syncthing ./cmd/syncthing
# CGO_ENABLED=1 GOOS=linux GOARCH=arm64 go build -tags noupgrade -trimpath -o syncthing ./cmd/syncthing
cp syncthing ../Risync/app/src/main/jniLibs/arm64-v8a/libsyncthing.so
```

**NOTE**: The above procedure assumes GitHub provides safe code from this repository and no verification mechanism is currently in place!
- The binary reports unknown-dev, building without build.go drops the version ldflags. Do not verify upgrades by the version shown in the GUI.