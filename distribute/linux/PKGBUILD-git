# Maintainer: Thomas Oster <thomas.oster@rwth-aachen.de>
pkgname=visicut-git
pkgver=20120824
pkgrel=1
pkgdesc="A userfriendly tool to create, save and send Jobs to a Lasercutter"
arch=(any)
url="http://visicut.org"
license=('LGPL')
groups=()
depends=('java-environment' 'bash')
makedepends=('git' 'apache-ant')
optdepends=('inkscape-extension-visicut: Use VisiCut directly from Inkscape')
provides=(visicut)
conflicts=(visicut)
replaces=()
backup=()
options=()
install=install
source=(plf-mime.xml)
noextract=()
md5sums=('023d09901b9075e86821a23953e82710')

_gitroot=git://github.com/t-oster/VisiCut.git
_gitname=develop
_gitbranch=develop

build() {
  cd "$srcdir"
  if [ -d "$_gitname" ]
  then
    echo "Removing old snapshot..."
    rm -rf "$_gitname"
  fi
  msg "Connecting to GIT server...."
  git clone --recursive --depth=1 -b "$_gitbranch" "$_gitroot" "$_gitname"

  msg "GIT checkout done or server timeout"
  msg "Starting build..."

  rm -rf "$srcdir/$_gitname-build"
  cp -r "$srcdir/$_gitname" "$srcdir/$_gitname-build"
  cd "$srcdir/$_gitname-build"
  # Update Version number to gitbuild
  VERSION=$(date +%Y%m%d)-git
  mv src/com/t_oster/visicut/gui/resources/VisicutApp.properties VisicutApp.properties
cat VisicutApp.properties|sed "s#version = .*#version = $VERSION#g#" > src/com/t_oster/visicut/gui/resources/VisicutApp.properties
rm VisicutApp.properties

  make
}

package() {
  cd "$srcdir/$_gitname-build"
  make DESTDIR="$pkgdir/" install
}

