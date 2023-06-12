#include "workers.h"  // Header-Datei, die Funktionen zur Verwaltung von Arbeitern enthält
#include "semaphores.h"  // Header-Datei, die Funktionen zur Verwaltung von Semaphoren enthält

#include <stdio.h>  // Standard-Input-Output-Bibliothek
#include <stdlib.h>  // Standardbibliothek, enthält Funktionen wie exit und random
#include <unistd.h>  // UNIX-Standardbibliothek, enthält Funktionen wie usleep
#include <time.h>  // Zeitbibliothek, enthält Funktionen wie time

// Array, das die Verfügbarkeit von "Stäbchen" darstellt (1 = verfügbar, 0 = nicht verfügbar)
volatile int staebchen[5]={1,1,1,1,1};
// Array, das die Besitzverhältnisse für die Philosophen darstellt (1 = hat Stäbchen, 0 = hat kein Stäbchen)
volatile int have_one[5]={0,0,0,0,0}; 
// Array von Semaphoren, eines für jedes Stäbchen
semaphore sem_staebchen[5];

// Setup-Funktion, wird vor dem Test ausgeführt
void test_setup(void) {
  printf("Test Setup\n");  // Debugging-Ausgabe
  readers=0;  // Anzahl der Leser initialisieren (in diesem Fall irrelevant)
  writers=5;  // Anzahl der Schreiber (Philosophen) auf 5 setzen
  srandom(time(NULL));  // Zufallszahlengenerator initialisieren

  // Semaphoren für jedes Stäbchen initialisieren
  for(int i = 0; i < 5; i++)
      sem_staebchen[i] = sem_init(1);
}

// End-Funktion, wird nach dem Test aufgerufen
void test_end(void) {
  printf("Test End\n");  // Debugging-Ausgabe
  // An dieser Stelle könnte man Ressourcen freigeben, z.B. Semaphoren zerstören
}

// Funktion, die aufgerufen wird, wenn ein "Leser" gestartet wird
// In diesem Kontext macht das keinen Sinn, daher wird das Programm beendet
void reader(long my_id) {
  printf("Wer hat mich da aufgerufen?\n");
  exit(1);
}

// Funktion zum "Nehmen" eines Stäbchens
// Gibt 1 zurück, wenn das Stäbchen erfolgreich genommen wurde, sonst 0
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

// Funktion zum "Weglegen" eines Stäbchens
// Überprüft, ob das Stäbchen tatsächlich genommen wurde und gibt dann das Semaphor frei
void staebchen_weglegen(int my_id, int pos) {
  printf("%i legt %i weg\n", my_id, pos);
  if (staebchen[pos]!=0) {
    printf("Fehler: staebchen[%i]=%i statt 0\n", pos, staebchen[pos]);
    exit(1);
  }
  staebchen[pos]++;
  sem_v(sem_staebchen[pos]);
}

// Funktion, die für jeden "Schreiber" (Philosoph) ausgeführt wird
// In einer Schleife versuchen die Philosophen, beide Stäbchen zu nehmen und dann zu essen
void writer(long long_my_id) {
  int my_id=long_my_id;  // Die ID des Philosophen
  int nxt=(my_id+1)%5;  // Die ID des "nächsten" Stäbchens (rechts)
  int i=100;  // Anzahl der Iterationen, die der Philosoph durchführen soll
  int links=0;  // Zustand des "linken" Stäbchens (1 = genommen, 0 = nicht genommen)
  int rechts=0;  // Zustand des "rechten" Stäbchens
  while (i>0) {
    if (!links) links=staebchen_nehmen(my_id, my_id);  // Versuche, das linke Stäbchen zu nehmen
    if (!rechts) rechts=staebchen_nehmen(my_id, nxt);  // Versuche, das rechte Stäbchen zu nehmen
    if (links && rechts) {  // Wenn beide Stäbchen genommen wurden...
      printf("%i futtert jetzt\n", my_id);  // ...dann füttere den Philosophen...
      usleep(random()%200);  // ...und warte eine zufällige Zeit...
      i--;  // ...und verringere die Anzahl der noch durchzuführenden Iterationen.
      staebchen_weglegen(my_id, my_id); links=0;  // Lege das linke Stäbchen zurück
      staebchen_weglegen(my_id, nxt);   rechts=0;  // Lege das rechte Stäbchen zurück
    } else {  // Wenn nicht beide Stäbchen genommen werden konnten...
      if (links) {
        staebchen_weglegen(my_id, my_id); links=0;  // ...lege das linke Stäbchen zurück, wenn es genommen wurde...
      }
      if (rechts) {
        staebchen_weglegen(my_id, nxt);   rechts=0;  // ...und das rechte, falls genommen.
      }
      usleep(random()%200); // warte, bevor du es erneut versuchst
    }
  }
}
