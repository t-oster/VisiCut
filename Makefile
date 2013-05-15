PREFIX?=/usr
all: jar

src/com/t_oster/visicut/gui/resources/splash.png: splashsource.svg src/com/t_oster/visicut/gui/resources/VisicutApp.properties
	./generatesplash.sh
jar: src/com/t_oster/visicut/gui/resources/splash.png
	ant jar
clean:
	ant clean
install:
	mkdir -p $(DESTDIR)$(PREFIX)/share
	cp -r dist $(DESTDIR)$(PREFIX)/share/visicut
	mkdir -p $(DESTDIR)$(PREFIX)/share/pixmaps
	cp icon.png $(DESTDIR)$(PREFIX)/share/pixmaps/visicut.png
	cp icon-48.png $(DESTDIR)$(PREFIX)/share/pixmaps/visicut-48.png
	cp icon-32.png $(DESTDIR)$(PREFIX)/share/pixmaps/visicut-32.png
	cp distribute/linux/visicut $(DESTDIR)$(PREFIX)/share/visicut/
	cp -r distribute/files/* $(DESTDIR)$(PREFIX)/share/visicut/
	mkdir -p $(DESTDIR)$(PREFIX)/share/visicut/inkscape_extension
	cp tools/inkscape_extension/*.inx $(DESTDIR)$(PREFIX)/share/visicut/inkscape_extension/
	cp tools/inkscape_extension/*.py $(DESTDIR)$(PREFIX)/share/visicut/inkscape_extension/
	mkdir -p $(DESTDIR)$(PREFIX)/share/visicut/illustrator_script
	cp tools/illustrator_script/*.scpt $(DESTDIR)$(PREFIX)/share/visicut/illustrator_script/
	mkdir -p $(DESTDIR)$(PREFIX)/bin
	ln -s $(PREFIX)/share/visicut/visicut $(DESTDIR)$(PREFIX)/bin/visicut
	mkdir -p $(DESTDIR)$(PREFIX)/share/applications
	cat distribute/linux/VisiCut.desktop | sed s#PREFIX#$(PREFIX)#g# > $(DESTDIR)$(PREFIX)/share/applications/VisiCut.desktop

uninstall:
	rm -rf $(PREFIX)/share/visicut
	rm -f $(PREFIX)/share/pixmaps/visicut.png
	rm -f $(PREFIX)/bin/visicut
	rm -f $(PREFIX)/share/applications/VisiCut.desktop
