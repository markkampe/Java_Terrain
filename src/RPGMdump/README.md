# RPGMdump
The tiles in RPGMaker maps are initialized in large (on a single line) arrays,
which makes it very difficult to find and check the tiles for a specified
location.  This program attempts to dump those arrays out in neat rows of
constant-width columns.
 
## Usage
```
    rpgmdump [-s] [-l level] [-w width]

   -s   ... suppress zero entries (just print blanks)
   -l # ... which RPGMaker map level to print (default: all)
   -w # ... output field width (default: 5)
```
