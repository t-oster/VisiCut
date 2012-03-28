# Maintainer: Thomas Oster <thomas.oster@rwth-aachen.de>
pkgname=inkscape-extension-visicut
pkgver=0.3
pkgrel=1
epoch=
pkgdesc="An extension to run VisiCut on all selected elements directly from Inkscape"
arch=(any)
url="http://visicut.org"
license=('LGPL')
groups=()
depends=(python2 inkscape python2-lxml visicut)
makedepends=()
checkdepends=()
optdepends=()
provides=()
conflicts=()
replaces=()
backup=()
options=()
install=
changelog=
source=(visicut_export.inx visicut_export.py daemonize.py)
noextract=()
md5sums=('6528d3855d29c6dbf214a2cfd821e89a'
         '21e1bb2ef5d882e68e7eb9b60d23d381'
         '2a711e173eeab0d913ed5a6e2b87e9fb')

package() {
  cd "$srcdir"
  mkdir -p "$pkgdir/usr/share/inkscape/extensions"
  cp visicut_export.inx "$pkgdir/usr/share/inkscape/extensions/"
  cp visicut_export.py "$pkgdir/usr/share/inkscape/extensions/"
  cp daemonize.py "$pkgdir/usr/share/inkscape/extensions/"
}
