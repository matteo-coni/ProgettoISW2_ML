package control;

import model.Issue;
import model.Release;

import java.util.ArrayList;
import java.util.List;

public class ProportionController {

    public static List<Issue> computeProportion(List<Release> releaseList, List<Issue> bugsList){

        List<Issue> bugsListProportion = new ArrayList<>();
        /*qui ora devo prendere la lista bugsList e verificare quali bug non hanno l'av e l'iv
          su quei bug devo calcolare l'iv (e poi l'avlist) in base al valore p calcolato o con cold start o con incremental
          p = (fv - iv)/(fv - ov) e iv = fv-(fv-ov)*p

          se uso cold start -> prendo alcuni progetti scelti, prendo i relativi bug che hanno iv ed av, e su essi mi calcolo p
          di ogni singolo bug e poi quello totale. successivamente prendo p_total di tutti i progetti e mi calcolo la mediana
          -qui bisogna fare attenzione a usare bug coerenti con le date

          se uso incremental -> calcolo p come la media tra i difetti fixati nelle versioni precedenti, riferita a quel progetto
          ovvero se sto considerando il bug senza iv numero 10, calcolo p di tutti i bug precedenti a 10 che avevano gia l'iv
          e faccio la media.

         */

        return bugsListProportion; //qui devo ritornare la lista dei bug con tutte le iv e av presenti, quindi quella definitiva

    }
}
