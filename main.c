#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "btlib.h"
#include "led-matrix-c.h"

typedef struct sColor
{
  uint8_t r;
  uint8_t g;
  uint8_t b;
  uint8_t test;
} sColor_t;

int le_callback(int clientnode,int operation,int cticn);
void vWritePixel(struct LedCanvas *canvas, u_int32_t u32PosX, u_int32_t u32PosY, sColor_t sPixelColor);

int main(int argc, char **argv)
{
  if(init_blue("devices.txt") == 0)
  {
    //Stop if couldn't initialise
    printf("Could not initalise Bluetooth. Stopping...\n");
    return(0);
  }
  printf("Bluetooth initalised successfully\n");
  struct RGBLedMatrixOptions options;
  struct RGBLedMatrix *matrix;
  struct LedCanvas *canvas;
  int width, height;

  //Parameters
  memset(&options, 0, sizeof(options));
  options.rows = 32;
  options.chain_length = 2;

  matrix = led_matrix_create_from_options(&options, &argc, &argv);
  if (matrix == NULL)
  {
    printf("Could not initialise matrix\n");
    return 1;
  }
    
  canvas = led_matrix_get_canvas(matrix);

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

  void vWritePixel(struct LedCanvas *canvas, u_int32_t u32PosX, u_int32_t u32PosY, sColor_t sPixelColor)
  {
    led_canvas_set_pixel(canvas, u32PosX, u32PosY, sPixelColor.r, sPixelColor.b, sPixelColor.g);
  }