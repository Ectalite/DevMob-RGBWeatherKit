#include <stdio.h>
#include <stdlib.h>
#include <string.h>
extern "C" {
#include "btlib.h"
}
#include "graphics.h"
#include "led-matrix.h"

typedef struct sColor
{
  uint8_t r;
  uint8_t g;
  uint8_t b;
} sColor_t;

typedef struct sMatrixObjects
{
  rgb_matrix::RGBMatrix *matrix;
  rgb_matrix::FrameCanvas *canvas;
  rgb_matrix::FrameCanvas *canvasTemp;
} sMatrixObjects_t;

int le_callback(int clientnode,int operation,int cticn, void *pvParameter);
void vWritePixel(sMatrixObjects_t*, uint8_t u8PosX, uint8_t u8PosY, sColor_t sPixelColor,bool bDisplayOnMatrix);
void vWriteText(sMatrixObjects_t*, uint8_t u8PosX, uint8_t u8PosY, sColor_t sTextColor, char cText[20],bool bDisplayOnMatrix);

int main(int argc, char **argv)
{
  if(init_blue((char*)("devices.txt")) == 0)
  {
    //Stop if couldn't initialise
    printf("Could not initalise Bluetooth. Stopping...\n");
    return(0);
  }
  printf("Bluetooth initalised successfully\n");
  rgb_matrix::RGBMatrix::Options matrix_options;
  rgb_matrix::RuntimeOptions runtime_opt;
  //struct RGBLedMatrixOptions options;
  //struct RGBLedMatrix *matrix;
  //int width, height;

  //Parameters
  //memset(&options, 0, sizeof(options));
  matrix_options.rows = 32;
  matrix_options.chain_length = 2;
  
  //matrix = led_matrix_create_from_options(&options, &argc, &argv);
  if (!rgb_matrix::ParseOptionsFromFlags(&argc, &argv, &matrix_options, &runtime_opt))
  {
    printf("Could not initialise matrix\n");
    return 1;
  }
  sMatrixObjects_t sMatrixObjects;

  sMatrixObjects.matrix = rgb_matrix::RGBMatrix::CreateFromOptions(matrix_options, runtime_opt);
  if (sMatrixObjects.matrix == NULL)
  {
    printf("Could not create a matrix pointer\n");
    return 1;
  }

  sMatrixObjects.canvas = sMatrixObjects.matrix->CreateFrameCanvas();

  printf("Device initalised successfully\n");
  list_ctics(1000,LIST_FULL); //node 1000 is me
  uint32_t ui32Handles = 327697; //0x00 05 00 11
  uint8_t *ui8Handles = (uint8_t*)&ui32Handles;

  le_server((int(*)())le_callback,0,(void*)&sMatrixObjects);
  
  close_all();
  return(0);
}

int le_callback(int clientnode,int operation,int cticn, void *pvParameter)
{  
  int nread;
  unsigned char dat[1];
  //struct RGBLedMatrix *matrix = (struct RGBLedMatrix *)pvParameter;
  sMatrixObjects_t* sMatrixObjects = (sMatrixObjects_t*)pvParameter;
  
  if(operation == LE_CONNECT)
  {
    printf("%s has connected\n",device_name(clientnode));
  }
  else if(operation == LE_WRITE)
  {
    printf("%s written by %s\n",ctic_name(localnode(),cticn),device_name(clientnode));
    
    //If Send Characteristic was written
    if(cticn == 4)
    {
      nread = read_ctic(localnode(),cticn,dat,sizeof(dat));
      // Check if value written is 1 -> data has been send 
      //Bit 1 is send, Bit 2 is display on matrix, Bit 3 is display Text or display one Pixel, Bit 4 clears canvas
      bool bSend = dat[0] & 0x01 << 0;
      bool bDisplay = dat[0] & 0x01 << 1;
      bool bText = dat[0] & 0x01 << 2;
      bool bClear = dat[0] & 0x01 << 3;
      if(bSend == 1)
      {
        printf("Send 1 was received setting it to 0\n");
        uint8_t ui8Reset = 0;
        uint8_t *pui8Reset = &ui8Reset;
        if(bClear)
        {
          printf("Clearing the matrix\n");
          //Clear Canvas and update matrix
          sMatrixObjects->canvas->Clear();
          (void)sMatrixObjects->matrix->SwapOnVSync(sMatrixObjects->canvas, 1);
        }
        else
        {
          int iError;
          unsigned char cBuffer[5];
          uint8_t u8PosX;
          uint8_t u8PosY;
          sColor_t sChoosedColor;
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

          if(!bText)
          {
            printf("PixelMode\n");
            vWritePixel(sMatrixObjects, u8PosX, u8PosY, sChoosedColor, bDisplay);
          }
          else
          {
            printf("TextMode\n");
            char cText[20];
            iError = read_ctic(localnode(),5,(unsigned char *)cText,19);
            if(ERROR_FATAL != iError)
            {
              printf("Text received: %s\n",cText);
            }
            else
            {
              printf("Fatal error by Text reading\n");
            }
            vWriteText(sMatrixObjects, u8PosX, u8PosY, sChoosedColor, cText, bDisplay);
          }
        }
        write_ctic(localnode(),cticn,pui8Reset,1);
      }
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

  void vWritePixel(sMatrixObjects_t *sMatrixObjects, uint8_t u8PosX, uint8_t u8PosY, sColor_t sPixelColor, bool bDisplayOnMatrix)
  {
    //struct LedCanvas *canvas;
    //canvas = led_matrix_get_canvas(matrix);
    
    //int width, height;
    int x, y, i;
    //led_canvas_get_size(canvas, &width, &height);
    //fprintf(stderr, "Size: %dx%d.\n", width, height);
    if(u8PosX < sMatrixObjects->canvas->width() && u8PosY < sMatrixObjects->canvas->height())
    {
      //led_canvas_set_pixel(canvas, u8PosX, u8PosY, sPixelColor.r, sPixelColor.g, sPixelColor.b);
      sMatrixObjects->canvas->SetPixel(u8PosX, u8PosY, sPixelColor.r, sPixelColor.g, sPixelColor.b);
      if(bDisplayOnMatrix == 1)
      {
        //canvas = led_matrix_swap_on_vsync(matrix, canvas);
        (void)sMatrixObjects->matrix->SwapOnVSync(sMatrixObjects->canvas, 1);
      }
    }
    else
    {
      printf("Error by vWritePixel\n");
    }
  }

  void vWriteText(sMatrixObjects_t *sMatrixObjects, uint8_t u8PosX, uint8_t u8PosY, sColor_t sTextColor, char cText[20],bool bDisplayOnMatrix)
  {
    //struct LedCanvas *canvas;
    //canvas = led_matrix_get_canvas(matrix);
    rgb_matrix::Color color(sTextColor.r, sTextColor.g, sTextColor.b);
    rgb_matrix::Color bg_color(0, 0, 0);
    int letter_spacing = 0;
    /*
     * Load font. This needs to be a filename with a bdf bitmap font.
     */
    rgb_matrix::Font font;
    char* bdf_font_file = (char*)("./fonts/4x6.bdf");
    if (!font.LoadFont(bdf_font_file)) {
      fprintf(stderr, "Couldn't load font '%s'\n", bdf_font_file);
    }
    //int width, height;
    int x, y, i;
    //led_canvas_get_size(canvas, &width, &height);
    //fprintf(stderr, "Size: %dx%d.\n", width, height);
    if(u8PosX < sMatrixObjects->canvas->width() && u8PosY < sMatrixObjects->canvas->height())
    {
      rgb_matrix::DrawText(sMatrixObjects->canvas, font, u8PosX, u8PosY + font.baseline(), color, &bg_color, cText, letter_spacing);
      if(bDisplayOnMatrix == 1)
      {
        //canvas = led_matrix_swap_on_vsync(matrix, canvas);
        (void)sMatrixObjects->matrix->SwapOnVSync(sMatrixObjects->canvas, 1);
      }
    }
    else
    {
      printf("Error by vWritePixel\n");
    }
  }