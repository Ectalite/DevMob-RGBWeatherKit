CC	 = gcc
CXX  = g++ -std=gnu++17
RM   = rm -f
LIB  = ./lib
INC  = -I . -I include/
OBJ  = main.o
OUTPUTDIR = ./bin/
MKDIR = mkdir -p $(OUTPUTDIR)
RGB_LIB_DISTRIBUTION = .
RGB_LIBDIR=$(RGB_LIB_DISTRIBUTION)/lib
RGB_LIBRARY_NAME=rgbmatrix
LDFLAGS+=-L$(RGB_LIBDIR) -l$(RGB_LIBRARY_NAME) -lrt -lm -lpthread

default: all

all: main

%.o: %.cc
	$(MAKE) -C $(LIB)
	$(CXX) $(INC) -c $<

main: $(OBJ)
	$(MKDIR)
	$(CC) $(INC) -c $(LIB)/btlib.c -o $(OUTPUTDIR)btlib.o
	$(CXX) $(OUTPUTDIR)btlib.o $< -o $@ $(LDFLAGS) -lstdc++

clean veryclean:
	$(RM) -rf $(OUTPUTDIR)
	$(RM) main
	$(RM) *.o
