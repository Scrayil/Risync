# SYNCTHING LIBRARY COMPILATIONS
```sh
cd /home/<USER>/Desktop/dev/git/syncthing
git checkout main
git pull
git checkout <LATEST_STABLE_TAG>
CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -tags noupgrade -trimpath -o syncthing ./cmd/syncthing
cp syncthing ../Risync/app/src/main/jniLibs/arm64-v8a/libsyncthing.so
```

**NOTE**: The above procedure assumes GitHub provides safe code from this repository and no verification mechanism is currently in place!
- The binary reports unknown-dev, building without build.go drops the version ldflags. Do not verify upgrades by the version shown in the GUI.