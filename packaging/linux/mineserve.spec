Name:           mineserve
Version:        1.8.0
Release:        1%{?dist}
Summary:        Native Linux Minecraft Java, Modded, Proxy, and Bedrock Server Manager
License:        GPL-3.0-or-later
URL:            https://github.com/devwithzachary/MineServe
BuildArch:      x86_64
AutoReqProv:    no

%description
MineServe is a free and open-source Minecraft server manager for Linux and Android.
It supports Paper, Purpur, Folia, Fabric, NeoForge, Forge, Vanilla, Velocity, BungeeCord,
and Bedrock servers with automatic portable OpenJDK provisioning, built-in Bore/Playit/UPnP
networking, an adaptive Compose Desktop GUI, and a headless Web Remote Admin daemon mode.

%install
mkdir -p %{buildroot}/opt/mineserve
cp -a %{_sourcedir}/mineserve-dist/* %{buildroot}/opt/mineserve/
mkdir -p %{buildroot}/usr/bin
ln -sf /opt/mineserve/bin/mineserve %{buildroot}/usr/bin/mineserve
install -Dm644 %{_sourcedir}/mineserve.desktop %{buildroot}/usr/share/applications/mineserve.desktop
install -Dm644 %{_sourcedir}/mineserve.png %{buildroot}/usr/share/icons/hicolor/256x256/apps/mineserve.png
install -Dm644 %{_sourcedir}/mineserve.service %{buildroot}/usr/lib/systemd/system/mineserve.service

%files
/opt/mineserve
/usr/bin/mineserve
/usr/share/applications/mineserve.desktop
/usr/share/icons/hicolor/256x256/apps/mineserve.png
/usr/lib/systemd/system/mineserve.service
