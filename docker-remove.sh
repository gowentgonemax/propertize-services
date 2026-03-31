#!/bin/bash

echo "Stopping Docker processes..."
pkill -9 Docker
pkill -9 com.docker
pkill -9 vpnkit
pkill -9 dockerd
pkill -9 containerd

echo "Removing Docker Desktop application..."
sudo rm -rf /Applications/Docker.app

echo "Removing Docker support files..."
rm -rf ~/Library/Containers/com.docker.docker
rm -rf ~/Library/Containers/com.docker.helper
rm -rf ~/Library/Application\ Support/Docker\ Desktop
rm -rf ~/Library/Application\ Support/com.docker.docker
rm -rf ~/Library/Group\ Containers/group.com.docker
rm -rf ~/Library/Logs/Docker\ Desktop
rm -rf ~/Library/Preferences/com.docker.docker.plist
rm -rf ~/Library/Saved\ Application\ State/com.electron.docker-frontend.savedState

echo "Removing CLI configs..."
rm -rf ~/.docker

echo "Removing Docker VM data..."
rm -rf ~/Library/Containers/com.docker.vmnetd
sudo rm -rf /var/run/docker.sock
sudo rm -rf /var/tmp/com.docker.vmnetd.socket

echo "Removing Docker binaries (if any)..."
sudo rm -f /usr/local/bin/docker
sudo rm -f /usr/local/bin/docker-compose
sudo rm -f /usr/local/bin/docker-credential-desktop
sudo rm -f /usr/local/bin/docker-credential-osxkeychain
sudo rm -f /usr/local/bin/com.docker.cli

echo "Removing Docker networking leftovers..."
sudo rm -rf /Library/PrivilegedHelperTools/com.docker.vmnetd
sudo rm -rf /Library/LaunchDaemons/com.docker.vmnetd.plist

echo "Docker has been fully removed from this system."
echo "You should restart your Mac before reinstalling Docker Desktop."
