#include "workers.h"
#include "semaphores.h"

#include <stdio.h>
#include <stdlib.h>

#define A_BIG_NUMBER 10000
#define WORKERS 10

//-----------------------------------------------------------------------------
// alle globalen variablen fuer die beiden worker hier definieren,
// alle unbedingt mit "volatile" !!!
//-----------------------------------------------------------------------------

volatile int global_var=0;

// semaphore declaration
semaphore mutex_semaphor[WORKERS];

//-----------------------------------------------------------------------------
// bevor der test beginnt wird test_setup() einmal aufgerufen
// - die variablen  readers  bzw.  writers  muessen gesetzt werden: wieviele
//   prozesse sollen parallel die funktionen reader bzw. writer bearbeiten?
//-----------------------------------------------------------------------------

void test_setup(void) {
  printf("Test Setup\n");
  global_var=0;
  readers=0;
  writers=WORKERS;

  // init sempahore
  for (int i = 0; i < WORKERS; i++) {
    mutex_semaphor[i] = sem_init(0);
  }
}

//-----------------------------------------------------------------------------
// wenn alle worker fertig sind, wird test_end() noch aufgerufen
//-----------------------------------------------------------------------------

void test_end(void) {
  printf("Test End\n");
  printf("global_var is %i, should be %i\n", global_var, A_BIG_NUMBER);
  if (global_var != A_BIG_NUMBER) {
    printf("failed\n");
  } else {			
    printf("ok\n");
  }
}

//-----------------------------------------------------------------------------
// alle 10 worker laufen parallel:
//-----------------------------------------------------------------------------

void reader(long my_id) {
  printf("Wer hat mich da aufgerufen? Nicht gut!\n");
  exit(1);
}

void writer(long my_id) {
  while(global_var != A_BIG_NUMBER) { 
    if(global_var % 10 != my_id) { // falscher Prozess
        sem_p(mutex_semaphor[my_id]); // blockiere Prozess
    }
    else{ // richtiger Prozess
  global_var ++;
	printf("%i ", global_var);
	sem_v(mutex_semaphor[global_var % 10]); // gebe nur nächsten Prozess frei
    }
    if (global_var == A_BIG_NUMBER) { // alle blockierten Prozesse wieder freigeben
      for(int i = 0; i < WORKERS; i++) {
        sem_v(mutex_semaphor[i]);
      }
    }
  }
}







