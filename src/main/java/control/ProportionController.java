package control;

import model.Issue;
import model.Release;

import java.io.IOException;
import java.util.*;

public class ProportionController {

    private static final int  THREESHOLDCOLDSTART = 5;

    /*
       è possibile fare una discussione sull'arrotondamento del valore di iv calcolato con proportion
       potremmo vedere in quanti casi viene approssimato per difetto e quanti per eccesso e magari fare
       una proporzione/studio e vedere se poteva essere approssimato sempre per difetto o eccesso
     */
    public static List<Issue> computeProportion(List<Release> releaseList, List<Issue> bugsList) throws IOException {

        List<Issue> bugsListProportion = new ArrayList<>();
        List<Issue> bugsListToDo = new ArrayList<>();
        List<Issue> bugsListWithIv = new ArrayList<>();
        /*
          Qui ora devo prendere la lista bugsList e verificare quali bug non hanno l'av e l'iv
          su quei bug devo calcolare l'iv (e poi l'avlist) in base al valore p calcolato o con cold start o con incremental
          p = (fv - iv)/(fv - ov) e iv = fv-(fv-ov)*p

          se uso cold start -> prendo alcuni progetti scelti, prendo i relativi bug che hanno iv e av, e su essi mi calcolo p
          di ogni singolo bug e poi quello totale. Successivamente prendo p_total di tutti i progetti e mi calcolo la mediana
          assunzione: non considero che una data di fix di un ticket di un progetto puo essere maggiore della data del ticket
          che sto calcolando

          Se uso incremental -> calcolo p come la media tra i difetti fixati nelle versioni precedenti, riferita a quel progetto
          ovvero se sto considerando il bug senza iv numero 10, calcolo p di tutti i bug precedenti a 10 che avevano gia l'iv
          e faccio la media. Attenzione che fv del bug per p sia prima dell'op del bug da calcolare
         */

        //increment
        float p;
        float pTot;
        float pColdStart;
        int ivId;
        int ovId;
        int fvId;
        int count;
        for (Issue bug : bugsList) {
            if (bug.getIv() == null) {
                bugsListToDo.add(bug);
            } else {
                bugsListWithIv.add(bug);
            }
        }

        //calcolo una sola volta
        pColdStart = calculatorColdStart();

        /*
          Uso due liste diverse per evitare che un p di un bug calcolato con proportion -
          mi aiuti a calcolarne un altro. Ovvero cosi dato un bug n. 10 calcolato con proportion,
          esso non potrà essere utilizzato per calcolare il bug n.15 sempre con proportion (<-esempio)
        */
        for (Issue bugsToDo : bugsListToDo){
           count = 0;
           pTot = 0;
           for (Issue bug : bugsListWithIv){
               /*
                 qui devo controllare che la FV di tutti i bug che uso per calcolare p
                 sia minore (temporalmente) della OV del bug che sto prendendo in considerazione
                 In altre parole, un bug, per poter essere utilizzato nel calcolo di p,
                 deve essere chiuso prima dell'apertura del ticket del bug considerato
               */
               if ((bug.getFv().getId() < bugsToDo.getOv().getId()) && bug.getFv().getId()!=bug.getOv().getId()){
                   ivId = bug.getIv().getId();
                   ovId = bug.getOv().getId();
                   fvId = bug.getFv().getId();

                   //per evitare lo zero al denominatore

                       p = pCalc(ivId, ovId, fvId);
                       count++;
                       pTot = pTot + p;

               }
           }
            pTot = pTot/count;

           /*
             faccio cold start se il numero dei ticket utilizzati per il calcolo
             di p è minore di 5
            */
           if(count < THREESHOLDCOLDSTART) {
               calculatorIV(bugsToDo,pColdStart, releaseList);
           } else {
               calculatorIV(bugsToDo, pTot, releaseList);

           }
       }

        return bugsListProportion; //qui devo ritornare la lista dei bug con tutte le iv e av presenti, quindi quella definitiva

    }

    public static void calculatorIV(Issue bugsToDo, float p, List<Release> releaseList){

        Release relIv = null;
        float ivIdB;
        int ovIdB = bugsToDo.getOv().getId();
        int fvIdB = bugsToDo.getFv().getId();
        if (fvIdB==ovIdB) {
            ivIdB = (fvIdB - p); // qui metto 1 per ovviare al problema di fv==ov, ma resta il problema di quando fv e ov sono uguali ad 1 e viene 1
        } else {
            ivIdB = fvIdB - (fvIdB - ovIdB) * p; //iv = fv-(fv-ov)*p
        }

        for(Release rel : releaseList){
            if(rel.getId() == Math.round(ivIdB)){ //Math.round per approssimare, difetto se <0.5, eccesso se >=0.5
                relIv = rel;
            }
        }
        if(relIv==null) relIv = releaseList.get(0); //se l'indice dell'iv viene 0, quindi una release che non esiste, la forza ad 1

        bugsToDo.setIv(relIv);

    }

    public static float calculatorColdStart() throws IOException {

        List<Float> pTot = new ArrayList<>();
        float p;
        float pColdStart;
        int count;
        float pProj;
        List<String> listProjName = new ArrayList<>();
        List<Issue> listIssueColdStart;


        listProjName.add("TAJO");
        listProjName.add("STORM");

        //for su tutti i progetti scelti
        for(String projName : listProjName) {
            //ora prendo i bug
            JiraController jiraControl = new JiraController();
            listIssueColdStart = jiraControl.getIssues(projName);
            count = 0;
            pProj = 0;
            for(Issue bug : listIssueColdStart){
                if(bug.getIv()!=null && bug.getOv()!=bug.getFv()){
                        p = pCalc(bug.getIv().getId(), bug.getOv().getId(), bug.getFv().getId());
                        count++;
                        pProj += p;

                }

            }

            pProj = pProj / count;
            pTot.add(pProj);
        }

        pTot.sort(Comparator.comparing(o -> o)); //ordino la lista dei p dei vari progetti
        //qui prendo la mediana
        if (pTot.size() % 2 == 0) {
            pColdStart = (pTot.get(pTot.size() / 2) + pTot.get(pTot.size() / 2 - 1)) / 2;
        } else {
            pColdStart = pTot.get(pTot.size() / 2);
        }

        return pColdStart;

    }
    public static float pCalc(int ivId, int ovId, int fvId){

        return (float)(fvId - ivId) / (fvId - ovId);

    }

    public void calculatorAvAfterProportion(List<Issue> bugsList, List<Release> releaseList){

        int idIv;
        int lastId;
        int i;

        for(Issue bug: bugsList){
            List<Release> listAv = new ArrayList<>();


            //in questo modo li ricalcolo tutti
                idIv = bug.getIv().getId();
                lastId = bug.getFv().getId() -1  ; //l'ultima prima della fv
                for(i=idIv; i<=lastId; i++){

                    for (Release rel : releaseList){
                        Release relAv;//null
                        if (rel.getId()==i){
                            relAv = rel;
                            listAv.add(relAv);
                            bug.setAv(listAv);
                        }
                    }
                }

        }


    }
}
