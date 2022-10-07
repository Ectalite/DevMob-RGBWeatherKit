CC   = gcc
RM   = rm -f
LIB  = ./lib
INC  = -I. -I include/
OBJ  = main.o
RGB_LIB_DISTRIBUTION = .
RGB_LIBDIR=$(RGB_LIB_DISTRIBUTION)/lib
RGB_LIBRARY_NAME=rgbmatrix
LDFLAGS+=-L$(RGB_LIBDIR) -l$(RGB_LIBRARY_NAME) -lrt -lm -lpthread

default: all

all: main

%.o: %.c
	$(MAKE) -C $(LIB)
	$(CC) $(INC) -c $<

main: $(OBJ)
	$(CC) $< -o $@ $(LDFLAGS) btlib.c -lstdc++

clean veryclean:
	$(RM) main
	$(RM) *.o
