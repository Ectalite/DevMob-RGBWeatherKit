CC   = gcc
CLIB = btlib.c
RM   = rm -f

default: all

all: devmob

devmob: main.c
	$(CC) $(CLIB) -o main main.c

clean veryclean:
	$(RM) main
