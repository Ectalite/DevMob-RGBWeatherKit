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
} sColor_t;

int le_callback(int clientnode,int operation,int cticn, void *pvParameter);
void vWritePixel(struct RGBLedMatrix *matrix, uint8_t u8PosX, uint8_t u8PosY, sColor_t sPixelColor);

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

  printf("Device initalised successfully\n");
  list_ctics(1000,LIST_FULL); //node 1000 is me
  uint32_t ui32Handles = 327697; //0x00 05 00 11
  uint8_t *ui8Handles = (uint8_t*)&ui32Handles;
  //memset(ui8Handles,327697,4); 
  printf("%x %x %x %x\n",ui8Handles[3],ui8Handles[2],ui8Handles[1],ui8Handles[0]);
  write_ctic(localnode(),5,(unsigned char*)ui8Handles,0);
  printf("Write worked fine\n");
  char dat[5];
  read_ctic(localnode(),5,dat,sizeof(dat));
  printf("%x %x %x %x\n",dat[3],dat[2],dat[1],dat[0]);
  le_server(le_callback,0,(void*)matrix);
  
  close_all();
  return(0);
}

int le_callback(int clientnode,int operation,int cticn, void *pvParameter)
{  
  int nread;
  char dat[1];
  struct RGBLedMatrix *matrix = (struct RGBLedMatrix *)pvParameter;
  
  if(operation == LE_CONNECT)
  {
    printf("%s has connected\n",device_name(clientnode));
  }
  else if(operation == LE_WRITE)
    {
    printf("%s written by %s\n",ctic_name(clientnode,cticn),device_name(clientnode));
    
    //If Send Characteristic was written
    if(cticn == 4)
    {
      nread = read_ctic(localnode(),cticn,dat,sizeof(dat));
      // Check if value written is 1
      if(dat[0] == 1)
        printf("Send 1 was received setting it to 0\n");
        int iError;
        char cBuffer[5];
        uint8_t u8PosX;
        uint8_t u8PosY;
        sColor_t sChoosedColor;
        uint8_t ui8Reset = 0;
        uint8_t *pui8Reset = (uint8_t*)&ui8Reset;
        iError = read_ctic(localnode(),1,cBuffer,1);
        if(ERROR_FATAL != iError)
        {
          u8PosX = (uint8_t)cBuffer[0];
          printf("u8PosX = %d\n",u8PosX);
        }
        else
        {
          printf("Fatal error by PosX reading\n");
        }
        iError = read_ctic(localnode(),2,cBuffer,1);
        if(ERROR_FATAL != iError)
        {
          u8PosY = (uint8_t)cBuffer[0];
          printf("u8PosY = %d\n",u8PosY);
        }
        else
        {
          printf("Fatal error by PosY reading\n");
        }
        iError = read_ctic(localnode(),3,cBuffer,4);
        if(ERROR_FATAL != iError)
        {
          sChoosedColor.r = (uint8_t)cBuffer[0];
          sChoosedColor.g = (uint8_t)cBuffer[1];
          sChoosedColor.b = (uint8_t)cBuffer[2];
          printf("sChoosedColor = r:%d g:%d b:%d\n",sChoosedColor.r,sChoosedColor.g,sChoosedColor.b);
        }
        else
        {
          printf("Fatal error by Color reading\n");
        }
        vWritePixel(matrix, u8PosX, u8PosY, sChoosedColor);
        write_ctic(localnode(),cticn,pui8Reset,1);
      }
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

  void vWritePixel(struct RGBLedMatrix *matrix, uint8_t u8PosX, uint8_t u8PosY, sColor_t sPixelColor)
  {
    struct LedCanvas *canvas;
    canvas = led_matrix_get_canvas(matrix);
    int width, height;
    int x, y, i;
    //led_canvas_get_size(canvas, &width, &height);
    //fprintf(stderr, "Size: %dx%d.\n", width, height);
    if(u8PosX < width && u8PosY < height&& canvas != NULL)
    {
      led_canvas_set_pixel(canvas, u8PosX, u8PosY, sPixelColor.r, sPixelColor.g, sPixelColor.b);
      canvas = led_matrix_swap_on_vsync(matrix, canvas);
    }
    else
    {
      printf("Error by vWritePixel\n");
    }
  }