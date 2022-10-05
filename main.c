#include <stdio.h>
#include <stdlib.h>
#include "btlib.h"

int le_callback(int clientnode,int operation,int cticn);

int main()
{
  if(init_blue("devices.txt") == 0)
    return(0);
  
  printf("Device initalised successfully\n");

  list_ctics(1000,LIST_FULL); //node 1000 is me
  le_server(le_callback,0);
  
  close_all();
  return(0);
}

int le_callback(int clientnode,int operation,int cticn)
  {  
  int nread;
  char dat[32];
  
  if(operation == LE_CONNECT)
  {
    printf("%s has connected\n",device_name(clientnode));
    write_node(clientnode,"TEST",200);
  }
  else if(operation == LE_TIMER) // every timerds deci-seconds 
    printf("Timer\n");
  else if(operation == LE_WRITE)
    {
    printf("%s written by %s\n",ctic_name(clientnode,cticn),device_name(clientnode));
      // read data just written to cticn
    nread = read_ctic(localnode(),cticn,dat,sizeof(dat));
      // execute code depending on data
    if(cticn == 2 && dat[0] == 4)
      printf("Value 4 written to first byte of characteristic index 2\n");
    }
  else if(operation == LE_READ)
    printf("%s read by %s\n",ctic_name(clientnode,cticn),device_name(clientnode));  
  else if(operation == LE_DISCONNECT)
    {
    if(clientnode == 3)
      return(SERVER_EXIT);  // stop server when node 3 disconnects  
    }
  return(SERVER_CONTINUE);
  }  