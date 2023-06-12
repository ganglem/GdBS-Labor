#include "workers.h"
#include "semaphores.h"

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>

volatile int staebchen[5]={1,1,1,1,1};
volatile int have_one[5]={0,0,0,0,0}; 
semaphore sem_staebchen[5];

void test_setup(void) {
  printf("Test Setup\n");
  readers=0;
  writers=5;
  srandom(time(NULL));

  for(int i = 0; i < 5; i++)
      sem_staebchen[i] = sem_init(1);
}

void test_end(void) {
  printf("Test End\n");
  // Note: assuming there's a function to destroy semaphores
}

void reader(long my_id) {
  printf("Wer hat mich da aufgerufen?\n");
  exit(1);
}

int staebchen_nehmen(int my_id, int pos) {
  int n=staebchen[pos];
  if (n==1) {
    sem_p(sem_staebchen[pos]);
    printf("%i nimmt %i\n", my_id, pos);
    staebchen[pos]--; 
    return 1;
  } else {
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
  sem_v(sem_staebchen[pos]);
}

void writer(long long_my_id) {
  int my_id=long_my_id;
  int nxt=(my_id+1)%5;
  int i=100;
  int links=0;  
  int rechts=0;
  while (i>0) {
    if (!links) links=staebchen_nehmen(my_id, my_id);
    if (!rechts) rechts=staebchen_nehmen(my_id, nxt);
    if (links && rechts) {
      printf("%i futtert jetzt\n", my_id);
      usleep(random()%200);
      i--;
      staebchen_weglegen(my_id, my_id); links=0;
      staebchen_weglegen(my_id, nxt);   rechts=0;
    } else {
      if (links) {
        staebchen_weglegen(my_id, my_id); links=0;
      }
      if (rechts) {
        staebchen_weglegen(my_id, nxt);   rechts=0;
      }
      usleep(random()%200); // wait before trying again
    }
  }
}
