# Maintainer: Thomas Oster
pkgname=visicut
pkgver=1.7
pkgrel=1
pkgdesc="A userfriendly tool to generate, save and send Jobs to a Lasercutter"
arch=(any)
url="http://visicut.org"
license=('LGPL')
groups=()
depends=('java-runtime' 'bash')
makedepends=('apache-ant')
checkdepends=()
optdepends=('inkscape-extension-visicut: Use VisiCut directly from Inkscape')
provides=()
conflicts=('visicut-git')
replaces=()
backup=()
options=()
install=install
changelog=
source=(
https://github.com/t-oster/VisiCut/archive/$pkgver.tar.gz 
plf-mime.xml
https://github.com/t-oster/LibLaserCut/archive/visicut$pkgver.tar.gz
)
md5sums=('e999d42ff2fe3eb0b67d2b2e15684030'
         '023d09901b9075e86821a23953e82710'
         'bdc355978fb4d356f6874bfbd05c4bf8')
_gitdir=t-oster-VisiCut-

build() {
  cd "$srcdir/${_gitdir}"*
  rm -rf lib/LibLaserCut
  echo "inserting LibLaserCut..."
  mv ../t-oster-LibLaserCut-* lib/LibLaserCut
  echo "Compiling..."
  make
}

package() {
  cd "$srcdir/${_gitdir}"*
  make DESTDIR="$pkgdir/" install
  cp "$srcdir/plf-mime.xml" "$pkgdir/usr/share/visicut/"
}
