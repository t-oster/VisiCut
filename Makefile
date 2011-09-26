PREFIX?=/usr
all: jar

jar:
	ant jar
clean:
	ant clean
install:
	mkdir -p $(DESTDIR)$(PREFIX)/share/visicut/
	mkdir -p $(DESTDIR)$(PREFIX)/share/visicut/lib/
	cp -r dist/* $(DESTDIR)$(PREFIX)/share/visicut/
	mkdir -p $(DESTDIR)$(PREFIX)/share/pixmaps
	cp icon.png $(DESTDIR)$(PREFIX)/share/pixmaps/visicut.png
	cp distribute/linux/visicut $(DESTDIR)$(PREFIX)/share/visicut/
	cp -r distribute/files/* $(DESTDIR)$(PREFIX)/share/visicut/
	mkdir -p $(DESTDIR)$(PREFIX)/bin
	ln -s $(PREFIX)/share/visicut/visicut $(DESTDIR)$(PREFIX)/bin/visicut
	mkdir -p $(DESTDIR)$(PREFIX)/share/applications
	cat distribute/linux/VisiCut.desktop | sed s#PREFIX#$(PREFIX)#g# > $(DESTDIR)$(PREFIX)/share/applications/VisiCut.desktop

uninstall:
	rm -rf $(PREFIX)/share/visicut
	rm -f $(PREFIX)/share/pixmaps/visicut.png
	rm -f $(PREFIX)/bin/visicut
	rm -f $(PREFIX)/share/applications/VisiCut.desktop
