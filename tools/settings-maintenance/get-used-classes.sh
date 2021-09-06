#!/bin/bash
set -e
set -o pipefail


echo "This script generates the list of classes used by all pre-defined VisiCut settings repositories."
echo "This is useful to debug serialization issues."
echo ""

cd $(dirname "$0")
tmpdir=$(mktemp -d)
srcdir="$(pwd)/../../src"

echo "Getting URLs"
cat $srcdir/main/java/de/thomas_oster/visicut/misc/LabSettings.java | grep -E '^\s*result\.add' | grep -E --only-match '(https:[^"]*)' > $tmpdir/urls
cat $tmpdir/urls

echo
echo "Downloading to $tmpdir"
mkdir $tmpdir/zipfiles
cd $tmpdir/zipfiles
cat $tmpdir/urls | xargs wget

echo
echo "Uncompressing"
i=1
mkdir "$tmpdir/uncompressed"
for f in $tmpdir/zipfiles/*; do
    mkdir $tmpdir/uncompressed/$i
    unzip -q $f -d $tmpdir/uncompressed/$i
    i=$(($i+1))
done

echo
echo "List of non-aliased classes: (may contain false-positives)"
echo
# get XML tags that contain class names with a dot
# <com.foo.bar
egrep --no-filename --only-match --binary-files=without-match -r  '<[^/> ]+\.[^> ]+' $tmpdir/uncompressed | sort | uniq
# <blabla class="com.foo.bar">
egrep --no-filename --only-match --binary-files=without-match -r  'class="[^"]+\.[^"]+"' $tmpdir/uncompressed | sort | uniq

echo
echo "List of aliased classes (approximate. For an exact list, check uses of xstream.alias() manually.)"
cd "$srcdir/.."
grep -r --no-filename '\.alias(' "src"
rm -r "$tmpdir"
