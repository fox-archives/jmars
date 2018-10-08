#############################################################################
##
## CONTROL SECTION FOR EACH JMARS EDITION 
##

# There's a group of control variables for each jmars jar file:
#
#     DEFINES is a list of the #define'd variables for the config file.
#     LIBS    is a list of the lib/blah.zip files needed.
#     EXCLUDE is a list of wildcards to exclude from that jar file.

## Libraries needed by all distributions
COMMON_LIBS= \
	BrowserLauncher2-1_3.jar \
	commons-beanutils-1.7.0.jar \
        commons-codec-1.9.jar \
	commons-collections-2.1.1.jar \
	commons-fileupload-1.2.2.jar \
	commons-io-2.1.jar \
	commons-logging-1.2.jar \
	cookformlayout-1.1.jar \
	cookswing-1.5.jar \
	cookxml-3.0.1.jar \
	dom4j-1.6.1.jar \
	forms-1.1.0.jar \
	hsqldb-1.8.0_10.jar \
	javacsv.jar \
	jaxen-1.1-beta-6.jar \
	jcommon-1.0.10.jar \
	jfreechart-1.0.16.jar \
	mysql-connector-java-5.1.5-bin.jar \
	openmap-4.6.5.jar \
	postgresql-8.0-311.jdbc2.jar  \
	stampserver.jar \
	swingx-1.0.jar \
	trove.zip \
	xstream-1.4.2.jar \
	xmlpull-1.1.3.1.jar \
	xpp3_min-1.1.4c.jar \
	backport-util-concurrent-3.0.jar \
	ehcache-2.7.1.jar \
	slf4j-api-1.6.6.jar \
	slf4j-jdk14-1.6.6.jar \
	jsr107cache-1.0.jar \
	jaxb-impl.jar \
	jsr173_1.0_api.jar \
	JavaAPIforKml.jar \
	jaxb-api.jar \
	activation.jar \
	jts-1.11.jar \
	gt-opengis-2.7.1.jar \
	gt-api-2.7.1.jar \
	gt-main-2.7.1.jar \
	gt-metadata-2.7.1.jar \
	gt-referencing-2.7.1.jar \
	gt-shapefile-2.7.1.jar \
	gt-epsg-wkt-2.7.1.jar \
	gt-data-2.7.1.jar \
	jsr-275-1.0-beta-2.jar \
	vecmath-1.3.2.jar \
	jai_imageio-1.1.jar \
	jai_codec-1.1.3.jar \
	jai_core-1.1.3.jar \
	je-4.0.103.jar \
	jel-2.0.1.jar \
	json.jar \
	httpclient-4.5.1.jar \
	httpclient-cache-4.5.1.jar \
	httpcore-4.4.4.jar \
	httpmime-4.5.1.jar \
	ws-commons-util-1.0.2.jar \
	xmlrpc-client-3.1.3.jar \
	xmlrpc-common-3.1.3.jar \
	xmlrpc-server-3.1.3.jar \
	2013_signalpro_mars_spectra_vis.jar \
	ini4j-0.5.2-SNAPSHOT.jar

jmars.jar:		DEFINES = FOR_THEMIS
jmars.jar:		LIBS = $(COMMON_LIBS)
jmars.jar:		EXCLUDE =

jmars-mro.jar:	DEFINES = FOR_MRO SKIPABOUT
jmars-mro.jar:	LIBS = $(COMMON_LIBS)
jmars-mro.jar:	EXCLUDE =

lroc-jmars.jar:		DEFINES = FOR_LROC
lroc-jmars.jar:		LIBS = $(COMMON_LIBS)
lroc-jmars.jar:		EXCLUDE = \
	edu/asu/jmars/layer/obs/mro/ \
	edu/asu/jmars/layer/obs/themis2/ \
	edu/asu/jmars/layer/obs/plannedstamps/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/Dune_Field.zip

public-jmars.jar:	DEFINES = PUBLIC_MARS
public-jmars.jar:	LIBS = $(COMMON_LIBS)
public-jmars.jar:	EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/vesta_d_D_oct_2011.zip
	
jmars-2035.jar:	DEFINES = PUBLIC_2035
jmars-2035.jar:	LIBS = $(COMMON_LIBS)
jmars-2035.jar:	EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/vesta_d_D_oct_2011.zip
	
msip-jmars.jar:	DEFINES = MSIP_MARS
msip-jmars.jar:	LIBS = $(COMMON_LIBS)
msip-jmars.jar:	EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/vesta_d_D_oct_2011.zip

msip_full-jmars.jar:	DEFINES = MSIP_MARS_FULL
msip_full-jmars.jar:	LIBS = $(COMMON_LIBS)
msip_full-jmars.jar:	EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/vesta_d_D_oct_2011.zip

public-moon-jmars.jar:	DEFINES = PUBLIC_MOON SKIPABOUT
public-moon-jmars.jar:	LIBS = $(COMMON_LIBS)
public-moon-jmars.jar:	EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/Dune_Field.zip \
	resources/vesta_d_D_oct_2011.zip
	
public-earth-jmars.jar: DEFINES = PUBLIC_EARTH SKIPABOUT
public-earth-jmars.jar: LIBS = ${COMMON_LIBS}
public-earth-jmars.jar: EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/Dune_Field.zip \
	resources/vesta_d_D_oct_2011.zip

jmars-dawn.jar: DEFINES = FOR_VESTA SKIPABOUT
jmars-dawn.jar: LIBS = ${COMMON_LIBS}
jmars-dawn.jar: EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/Dune_Field.zip
	
jmars-demo.jar: DEFINES = FOR_DEMO SKIPABOUT
jmars-demo.jar: LIBS = ${COMMON_LIBS}
jmars-demo.jar: EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/Dune_Field.zip
		
public-vesta.jar: DEFINES = PUBLIC_VESTA SKIPABOUT
public-vesta.jar: LIBS = ${COMMON_LIBS}
public-vesta.jar: EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/Dune_Field.zip
	
public-ceres.jar: DEFINES = PUBLIC_CERES SKIPABOUT
public-ceres.jar: LIBS = ${COMMON_LIBS}
public-ceres.jar: EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/Dune_Field.zip
	
public-moj-jmars.jar: DEFINES = PUBLIC_MOJ SKIPABOUT
public-moj-jmars.jar: LIBS = ${COMMON_LIBS}
public-moj-jmars.jar: EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/Dune_Field.zip \
	resources/vesta_d_D_oct_2011.zip

public-mercury-jmars.jar: DEFINES = PUBLIC_MERCURY SKIPABOUT
public-mercury-jmars.jar: LIBS = ${COMMON_LIBS}
public-mercury-jmars.jar: EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/Dune_Field.zip \
	resources/vesta_d_D_oct_2011.zip
	
public-venus-jmars.jar: DEFINES = PUBLIC_VENUS SKIPABOUT
public-venus-jmars.jar: LIBS = ${COMMON_LIBS}
public-venus-jmars.jar: EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/Dune_Field.zip \
	resources/vesta_d_D_oct_2011.zip

public-mos-jmars.jar: DEFINES = PUBLIC_MOS SKIPABOUT
public-mos-jmars.jar: LIBS = ${COMMON_LIBS}
public-mos-jmars.jar: EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/Dune_Field.zip \
	resources/vesta_d_D_oct_2011.zip

public-outer_planet_moons-jmars.jar: DEFINES = PUBLIC_OUTER SKIPABOUT
public-outer_planet_moons-jmars.jar: LIBS = ${COMMON_LIBS}
public-outer_planet_moons-jmars.jar: EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/vesta_d_D_oct_2011.zip
	
for_asteroid-jmars.jar: DEFINES = FOR_ASTEROID SKIPABOUT
for_asteroid-jmars.jar: LIBS = ${COMMON_LIBS}
for_asteroid-jmars.jar: EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/vesta_d_D_oct_2011.zip

for_orex_spoc-jmars.jar: DEFINES = FOR_OREX SKIPABOUT
for_orex_spoc-jmars.jar: LIBS = ${COMMON_LIBS}
for_orex_spoc-jmars.jar: EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/vesta_d_D_oct_2011.zip
	
jmars2020.jar:	DEFINES = PUBLIC_2020
jmars2020.jar:	LIBS = $(COMMON_LIBS)
jmars2020.jar:	EXCLUDE = \
	edu/asu/jmars/layer/obs/ \
	edu/asu/jmars/layer/rawimage/ \
	edu/asu/jmars/layer/mroobs/ \
	resources/vesta_d_D_oct_2011.zip
	
#############################################################################

# The path separator for java classpaths
S := :

TMPDIR := .build-tmp
CLSDIR := .class-tmp

PACKAGES_CMD := find . -name '*.java' | perl -ne 's|./(.+)/[^/]+$$|$$1\n|;y|/|.|;$$p{$$_}++||print' | grep -v '\.\.'
DOCCLASSES := find edu/asu/jmars -type d | grep -v '\/\.' | sed 's/\//\./g'
#find edu/asu/jmars -type d -not -name CVS | perl -e '<"$_/*.java">&&y{/}{.},print'
#find -name '*.java' | perl -ne 's|./(.+)/[^/]+$|$1\n|;y|/|.|;$p{$_}++||print'


LIBRARIES = $(wildcard lib/*.zip)

# Programs (with common options):
SHELL		= /bin/sh
RM              = rm -f
MV              = mv -f
SED		= sed
ETAGS		= etags
XARGS		= xargs
CAT		= cat
FIND            = find
CPP		= cpp -C -P

INSTALL         = install
INSTALL_PROG    = $(INSTALL) -m $(MODE_PROGS)
INSTALL_FILE    = $(INSTALL) -m $(MODE_FILES)
INSTALL_DIR     = $(INSTALL) -m $(MODE_DIRS) -d

# Install modes
MODE_PROGS      = 555
MODE_FILES      = 444
MODE_DIRS       = 2755

# Build programs
JAVAC           = javac
JAVADOC         = javadoc
JAR             = jar

# Build flags
ALL_LIB_ZIPS    = $(sort $(wildcard lib/*.zip lib/*.jar))
CLASSPATH       = $(subst $S ,,$(subst $S ,$S,$(addsuffix $S,$(ALL_LIB_ZIPS))) )

JAVAC_FLAGS     = -g -deprecation -d $(CLSDIR) -J-Xms256m  -J-Xmx2G 
JAVADOC_FLAGS   = -link http://java.sun.com/j2se/1.5/docs/api/ -use -package
JAR_FLAGS       = cfm
JIKES_DEP_FLAG	= +M

# Perl one-liner to find outdated java files that need recompiling
CMD_FIND_JAVAS = perl -e 'print join" ",grep{chop,/\.java/;(stat)[9]>((stat"$(CLSDIR)/$$`.class")[9]||0)}`find edu/asu/jmars net/signalpro -name "*.java"`'

JAVA_FILES = $(shell $(CMD_FIND_JAVAS))
CLASS_FILES = $(addprefix $(CLSDIR)/,$(JAVA_FILES:.java=.class))

# ------------------------------------------------------------------- #

# Prefix for every install directory
PREFIX		=

# Where to start installing the class files. Set this to an empty value
#  if you dont want to install classes
CLASS_DIR	= $(PREFIX)classes

# The directory to install the jar file in. Set this to an empty value
#  if you dont want to install a jar file
JAR_DIR	        = $(PREFIX)lib

# The directory to install html files generated by javadoc
DOC_DIR         = $(PREFIX)docs

# The directory to install script files in
SCRIPT_DIR	= $(PREFIX)bin

# ------------------------------------------------------------------- #

# Everything
ALL_JAR_FILES = \
	jmars.jar \
	jmars-mro.jar \
	msip-jmars.jar \
	msip_full-jmars.jar \
	public-jmars.jar \
	lroc-jmars.jar \
	public-moon-jmars.jar \
	public-earth-jmars.jar \
	jmars-dawn.jar \
	public-mercury-jmars.jar \
	public-moj-jmars.jar \
	public-mos-jmars.jar \
	public-outer_planet_moons-jmars.jar \
	for_asteroid-jmars.jar \
	for_orex_spoc-jmars.jar \
	public-vesta.jar \
	public-ceres.jar \
	jmars2020.jar \
	for_bennu-jmars.jar \
	public-venus-jmars.jar \
	jmars-2035.jar \
	jmars-demo.jar

# Everything the jar files depend on
JAR_FILES_DEPS = $(patsubst %.java,%.class,$(wildcard *.java \
					resources/* \
					images/* \
					$(LIBRARIES) \
					Makefile )) \
					$(CLASS_FILES)

JMARS_CONFIG = resources/jmars.config
JMARS_USAGE = resources/usage

# ------------------------------------------------------------------- #


# Packages we should compile
PACKAGES = student

# Resource packages
RESOURCES =


# Directories with shell scripts
SCRIPTS =

# ------------------------------------------------------------------- #

# A marker variable for the top level directory
TOPLEVEL	:= .

# Subdirectories with java files:
JAVA_DIRS	:= $(subst .,/,$(PACKAGES)) $(TOPLEVEL)

# Subdirectories with only resource files:
RESOURCE_DIRS	:= $(subst .,/,$(RESOURCES))

# All the .xjava source files:
XJAVA_SRC	:= $(foreach dir, $(JAVA_DIRS), $(wildcard $(dir)/*.xjava))

# All the xjava files to build
XJAVA_OBJS	:= $(XJAVA_SRC:.xjava=.java)

# All the .java source files:
JAVA_SRC	:= $(foreach dir, $(JAVA_DIRS), $(wildcard $(dir)/*.java))
JAVA_SRC	:= $(XJAVA_OBJS) $(JAVA_SRC)

# Dependency files:
DEPEND_OBJS	:= $(JAVA_SRC:.java=.u)

# Objects that should go into the jar file. (find syntax)
JAR_OBJS	:= \( -name '*.class' -o -name '*.au' \
		       -o -name '*.properties' -o -name '*.cert' \)

# The intermediate java files and main classes we should build:
JAVA_OBJS	:= $(XJAVA_OBJS) $(JAVA_SRC:.java=.class)

# Resource files:
#  Extend the list to install other files of your choice
RESOURCE_SRC	:= *.properties *.gif *.au
#  Search for resource files in both JAVA_DIRS and RESOURCE_DIRS
RESOURCE_OBJS	:= $(foreach dir, $(JAVA_DIRS) $(RESOURCE_DIRS), \
		     $(wildcard $(foreach file, $(RESOURCE_SRC), \
		     $(dir)/$(file))))

# All the shell scripts source
SCRIPT_SRCS 	:= $(foreach dir, $(SCRIPTS), $(wildcard $(dir)/*.sh))
# All shell scripts we should install
SCRIPT_OBJS    	:= $(SCRIPT_SRCS:.sh=)

# All the files to install into CLASS_DIR
INSTALL_OBJS	:= $(foreach dir, $(JAVA_DIRS), $(wildcard $(dir)/*.class))
# Escape inner class delimiter $
INSTALL_OBJS	:= $(subst $$,\$$,$(INSTALL_OBJS))
# Add the resource files to be installed as well
INSTALL_OBJS	:= $(INSTALL_OBJS) $(RESOURCE_OBJS)

# MRO Regression Test
TESTMRO_DIR	= testmro

# ------------------------------------------------------------------- #

define check-exit
|| exit 1

endef

.SUFFIXES: .java .class .u .xjava .jar .zip

# -----------
# Build Rules
# -----------

RETRO = java -cp /themis/lib/retroguard.jar:$(CLSDIR)

# -------
# Targets
# -------

.PHONY: all jar install uninstall doc clean depend tags testmro testmro_clean testmro_prog
.SILENT: testmro testmro_clean testmro_prog
 
all::	src $(CLASS_FILES) testmro_prog

testmro_prog:
	-if test -d $(TESTMRO_DIR); then cd $(TESTMRO_DIR); make; fi

testmro_clean:
	-if test -d $(TESTMRO_DIR); then cd $(TESTMRO_DIR); make clean; fi

testmro:
	-if test -d $(TESTMRO_DIR); then cd $(TESTMRO_DIR); make runtest; fi

CMD_FIND_PACKS := -name \*.java|perl -ne'(($$x)=/^package (.+);/)&&last for(`cat $$_`);$$x=~y-.-/-;/^$$x\//||print"PACKAGE NAMING PROBLEM IN: $$_"'

find_package_problems:
	@find edu $(CMD_FIND_PACKS)
	@cd testmro;find testmro $(CMD_FIND_PACKS)

src: edu
	ln -s edu/asu/jmars src

#%.class: %.java
$(CLASS_FILES): $(JAVA_FILES)
	mkdir -p $(CLSDIR)
	$(CMD_FIND_JAVAS) | xargs $(JAVAC) $(JAVAC_FLAGS) -classpath '$(CLASSPATH)$S.'

define build-about
	date > resources/about.txt
	date '+%s' >> resources/about.txt
	find edu -name '*.java' | xargs cat | wc -l >> resources/about.txt
	find edu -name '*.java' | wc -l >> resources/about.txt
	find $(CLSDIR) -name '*.class' | wc -l >> resources/about.txt
endef

help:
	@echo "Usage: make {all|jar|install|uninstall|docs|clean|depend|tags}"

jar:  jmars.jar
jars: $(ALL_JAR_FILES)

jmars_deploy: jmars.jar public-jmars.jar msip_full-jmars.jar public-earth-jmars.jar public-vesta.jar public-ceres.jar

reverse = $(shell perl -e 'print join" ",reverse @ARGV' $(1))

source:
	tar czf source.tgz Makefile runme mro/jmars-mro `ls -d resources/* | grep -v \.dat` images edu lib

# Used to create the $(TMPDIR), specifically not implemented as a dependency.
define build-TMPDIR
	@echo
	echo '=== Creating build directory ($(TMPDIR))...'
	echo 
	echo '	-> Removing previous build'
	rm -rf $(TMPDIR)
	mkdir -p $(TMPDIR)
	echo '	-> Adding in-house class/resource files'
	cp -r resources images $(CLSDIR)/edu $(TMPDIR)
	cp -r $(CLSDIR)/net $(TMPDIR)
	$(JMARS_CONFIG) $(addprefix -D,$(DEFINES)) > $(TMPDIR)/$(JMARS_CONFIG)
	perl -ne 'if(/^\s*config\.url\.(\S+)\s+(.+)/){print STDERR "\t-> Appending $$1 config file\n";print"## FROM $$1 $$2\n",`wget -q -O - "$$2?jmars_config=1"`}' < $(TMPDIR)/$(JMARS_CONFIG) >> $(TMPDIR)/$(JMARS_CONFIG)
	cd $(TMPDIR); $(foreach x,$(call reverse,$(LIBS)),echo '	-> Adding library: $(x)';$(JAR) xf ../lib/$(x);)
	ls -1 edu/asu/jmars/layer/tes6/*.txt edu/asu/jmars/layer/tes6/ehcache.x* | cpio -pd $(TMPDIR)
	echo '	-> Trimming the fat'
	find $(TMPDIR) -mindepth 1 -name '*.java' -o -name CVS -o -name '.*' -o -name '*~' | xargs rm -rf
	rm -rf $(addprefix $(TMPDIR)/,$(EXCLUDE))
endef

# rule to build jar distributions
$(ALL_JAR_FILES): $(LIBRARIES) $(JAR_FILES_DEPS)
	@echo 
	@echo '========== Making $@ =========='
	@echo
	@echo '=== Building about.txt file...'
	$(build-about)
	$(build-TMPDIR)
	@echo
	@echo '=== Building the jar file...'
	@echo 
	$(JAR) cmf manifest $@ -C $(TMPDIR) .
	@echo
	@echo 'Signing $@'
	@echo
	cp $@ $@.tmp
#	jarsigner -keystore util/selfSignedCert.x509 -storepass password -signedjar $@.tmp $@ JMARS
	jarsigner -tsa http://timestamp.comodoca.com/rfc3161 -keystore util/jmars.keystore -storepass byodev -signedjar $@.tmp $@ jmars
	mv $@.tmp $@

mro:	jmars-mro.jar
	@echo
	@echo === Constructing jmars`date '+%y%m%d'`.zip for MRO...
	@echo
	rm -rf .mro jmars`date '+%y%m%d'`.zip
	mkdir -p .mro
	cp mro/README.txt mro/*.doc mro/*.docx .mro
	mkdir -p .mro/lib
	mv jmars-mro.jar .mro/lib/jmars.jar
	mkdir -p .mro/src
	cp -r Makefile edu images runme resources util .mro/src
	cp -r lib .mro/src/lib
	rm -rf .mro/src/resources/*.dat
	rm -rf .mro/src/resources/*.zip
	rm -f .mro/src/resources/jmars.config
	cd .mro/src && jar -xf ../../.mro/lib/jmars.jar resources/jmars.config
	mkdir -p .mro/bin
	cp mro/jmars mro/jmars-mro .mro/bin
	find .mro -mindepth 1 -name CVS -o -name '.*' -o -name '*~' \
		| xargs rm -rf
	cd .mro ; zip -qqr ../jmars`date '+%y%m%d'`.zip *
	@echo
	@echo ======== DONE! jmars`date '+%y%m%d'`.zip
	@echo

dawn:	jmars-dawn.jar
	@echo
	@echo === Constructing dawn`date '+%y%m%d'`.zip for Dawn...
	@echo
	rm -rf .dawn dawn`date '+%y%m%d'`.zip
	mkdir -p .dawn
	cp dawn/README.txt .dawn
	mkdir -p .dawn/lib
	mv jmars-dawn.jar .dawn/lib/jmars-dawn.jar
	mkdir -p .dawn/bin
	cp dawn/jasteroid .dawn/bin
	find .dawn -mindepth 1 -name CVS -o -name '.*' -o -name '*~' \
		| xargs rm -rf
	cd .dawn ; zip -qqr ../dawn`date '+%y%m%d'`.zip *
	@echo
	@echo ======== DONE! dawn`date '+%y%m%d'`.zip
	@echo

dawn_clean:
	rm -rf .dawn

# Doc target <-- auto-created by mmake, slightly modified manually on 6/21/01
#ifneq ($(strip $(PACKAGES)),)
docs:	docs/index.html

docs/index.html: Makefile $(shell find edu/asu/jmars -name "*.java")
	@echo "===> [Generating documentation, entry point is $(DOC_DIR)/index.html] "
	$(INSTALL_DIR) $(DOC_DIR) $(check-exit)
	$(DOCCLASSES) | xargs $(JAVADOC) -windowtitle "JMARS javadocs" -doctitle "JMARS" -classpath ".$S$(CLSDIR)$S$(CLASSPATH)" -d $(DOC_DIR) $(JAVADOC_FLAGS)
	@echo "===> [Documentation generated, entry point is $(DOC_DIR)/index.html] "

# Various cleanup routines
clean::	testmro_clean dawn_clean
	rm -rf *.class $(CLSDIR) $(TMPDIR) .mro
	rm -R *.* edu/asu/jmars/test
