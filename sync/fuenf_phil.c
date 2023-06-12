#include "workers.h"
#include "semaphores.h"

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>
#include <semaphore.h>

//-----------------------------------------------------------------------------
// alle globalen variablen fuer die beiden worker hier definieren,
// alle unbedingt mit "volatile" !!!
//-----------------------------------------------------------------------------

volatile int staebchen[5]={1,1,1,1,1};
volatile int have_one[5]={0,0,0,0,0}; // nur zur deadlock erkennung
sem_t sem_staebchen[5];

//-----------------------------------------------------------------------------
// bevor der test beginnt wird test_setup() einmal aufgerufen
//-----------------------------------------------------------------------------

void test_setup(void) {
  printf("Test Setup\n");
  readers=0;
  writers=5;
  srandom(time(NULL));

  for(int i = 0; i < 5; i++)
      sem_init(&sem_staebchen[i], 0, 1);
}

//-----------------------------------------------------------------------------
// wenn beider worker fertig sind wird test_end() noch aufgerufen
//-----------------------------------------------------------------------------

void test_end(void) {
  printf("Test End\n");
  
  for(int i = 0; i < 5; i++)
      sem_destroy(&sem_staebchen[i]);
}

//-----------------------------------------------------------------------------
// die beiden worker laufen parallel:
//-----------------------------------------------------------------------------

void reader(long my_id) {
  printf("Wer hat mich da aufgerufen?\n");
  exit(1);
}

int staebchen_nehmen(int my_id, int pos) {
  sem_wait(&sem_staebchen[pos]);

  int n=staebchen[pos];
  if (n==1) {
    printf("%i nimmt %i\n", my_id, pos);
    staebchen[pos]--; 
    return 1;
  } else {
    sem_post(&sem_staebchen[pos]);
    return 0;
  }
}

void staebchen_weglegen(int my_id, int pos) {
  printf("%i legt %i weg\n", my_id, pos);
  if (staebchen[pos]!=0) {
    printf("Fehler: staebchen[%i]=%i statt 0\n", pos, staebchen[pos]);
    exit(1);
  }
  staebchen[pos]++;
  sem_post(&sem_staebchen[pos]);
}

void writer(long long_my_id) {
  int my_id=long_my_id;
  int nxt=(my_id+1)%5;

  int i=100;
  int links=0;  
  int rechts=0;
  while (i>0) {
    if (!links && random()%10==7) links=staebchen_nehmen(my_id, my_id);
    if (!rechts && random()%10==7) rechts=staebchen_nehmen(my_id, nxt);

    if (links && rechts) {
      printf("%i futtert jetzt\n", my_id);
      usleep(random()%200);
      i--;
      staebchen_weglegen(my_id, my_id); links=0;
      staebchen_weglegen(my_id, nxt);   rechts=0;
    }

    have_one[my_id] = links || rechts;
    if (have_one[0]+have_one[1]+have_one[2]+have_one[3]+have_one[4]==5) {
      printf("%i gibt freiwillig ab\n", my_id);
      if (links) {
        staebchen_weglegen(my_id, my_id); links=0;
      }
      if (rechts) {
        staebchen_weglegen(my_id, nxt);   rechts=0;
      }
      have_one[my_id] = 0;
    }
    usleep(random()%200);
  }
}
