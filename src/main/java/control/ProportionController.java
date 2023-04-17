package control;

import model.Issue;
import model.Release;

import java.io.IOException;
import java.util.*;

public class ProportionController {

    public static List<Issue> computeProportion(List<Release> releaseList, List<Issue> bugsList) throws IOException {

        List<Issue> bugsListProportion = new ArrayList<>();
        List<Issue> bugsListToDo = new ArrayList<>();
        List<Issue> bugsListWithIv = new ArrayList<>();
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
        //increment
        float p = 0;
        float p_tot = 0;
        float pColdStart = 0;
        int ivId;
        int ovId;
        int fvId;
        int count = 0;
        for (Issue bug : bugsList) {
            if (bug.getIv() == null) {
                bugsListToDo.add(bug);
            } else {
                bugsListWithIv.add(bug);
            }
        }
        pColdStart = calculatorColdStart();
       for (Issue bugsToDo : bugsListToDo){         //uso due liste diverse per evitare che un p di un bug calcolato mi aiuti a calcolarne un'altra
           count = 0;
           p_tot = 0;
           for (Issue bug : bugsListWithIv){
               if (bug.getNum() < bugsToDo.getNum()){
                   ivId = bug.getIv().getId();
                   ovId = bug.getOv().getId();
                   fvId = bug.getFv().getId();
                   if (fvId != ovId) { //per evitare lo zero
                       p = pCalc(ivId, ovId, fvId);
                       count++;
                       p_tot = p_tot + p;
                   }
               }
           }
           p_tot = (float)(p_tot/count);
           if(count < 5) {
               //calculatorColdStart(bugsToDo, releaseList); //se count==0 significa che non c'è nessun bug con iv valido prima di esso e va applicato cold start
               calculatorIV(bugsToDo,pColdStart, releaseList);
           } else {
               System.out.println("bug: " + bugsToDo.getKey() + " p: " + p_tot); //prova stampa p
               calculatorIV(bugsToDo, p_tot, releaseList);

           }
       }

        return bugsListProportion; //qui devo ritornare la lista dei bug con tutte le iv e av presenti, quindi quella definitiva

    }

    public static void calculatorIV(Issue bugsToDo, float p, List<Release> releaseList){
        //System.out.println("entrato in calcultaor iv bug: " + bugsToDo.getKey() + " " + p) ;
        Release relIv = null;
        float ivIdB;
        int ovIdB = bugsToDo.getOv().getId();
        int fvIdB = bugsToDo.getFv().getId();
        if (fvIdB==ovIdB) {
            ivIdB = (fvIdB - (float) (1) * p); // qui metto 1 per ovviare al problema di fv==ov, ma resta il problema di quando fv e ov sono uguali ad 1 e viene 1
        } else {
            ivIdB = fvIdB - (fvIdB - ovIdB) * p; //iv = fv-(fv-ov)*p se ov e fv sono uguali fv-ov si annulla e viene sempre iv = fv
        }

        for(Release rel : releaseList){
            if(rel.getId() == Math.round(ivIdB)){ //Math.round per approssimare difetto se <0.5, eccesso se >=0.5
                relIv = rel;
            }
        }
        if(relIv==null) relIv = releaseList.get(0); //se l'indice dell'iv viene 0, quindi una release che non esiste, la forza ad 1

        bugsToDo.setIv(relIv);

    }

    public static float calculatorColdStart(/*Issue bugsToDo, List<Release> releaseList*/) throws IOException {

        //System.out.println("entrato in cold start, bug: " + bugsToDo.getKey());
        List<Float> p_tot = new ArrayList<>();
        float p;
        float p_coldStart;
        int count;
        float p_proj;
        List<String> listProjName = new ArrayList<>();
        //List<Release> listReleaseColdStart = new ArrayList<>();
        List<Issue> listIssueColdStart = new ArrayList<>();
        List<Issue> listIssueCSWithIV = new ArrayList<>();
        //todo: aggiungere enum oppure aggiungi altri progetti alla lisfa
        //listProjName.add("ZOOKEEPER");
        //listProjName.add("AVRO");
        listProjName.add("STORM");
        //for su tutti i progetti scelti, per ora con lista
        for(String projName : listProjName) {
            //ora prendo le release
            JiraController jiraControl = new JiraController();
            //listReleaseColdStart = jiraControl.getReleases(projName);
            listIssueColdStart = jiraControl.getIssues(projName);
            count = 0;
            p_proj = 0;
            for(Issue bug : listIssueColdStart){
                if(bug.getIv()!=null){
                    if(bug.getOv()!=bug.getFv()){
                        p = pCalc(bug.getIv().getId(), bug.getOv().getId(), bug.getFv().getId());
                        count++;
                        p_proj += p;
                    }
                }

            }

            p_proj = p_proj / count;
            p_tot.add(p_proj);
        }

        p_tot.sort(Comparator.comparing(o -> o));



        if (p_tot.size() % 2 == 0) {
            p_coldStart = (p_tot.get(p_tot.size() / 2) + p_tot.get(p_tot.size() / 2 - 1)) / 2;
        } else {
            p_coldStart = p_tot.get(p_tot.size() / 2);
        }

        return p_coldStart;

    }
    public static float pCalc(int ivId, int ovId, int fvId){
        float p = (float)(fvId - ivId) / (fvId - ovId);

        return p;
    }

    public void calculatorAvAfterProportion(List<Issue> bugsList, List<Release> releaseList){

        int idIv;
        int lastId;
        int i;



        for(Issue bug: bugsList){
            List<Release> listAv = new ArrayList<>();

            if (bug.getAv().isEmpty()){
                //System.out.println(bug.getAV();
                idIv = bug.getIv().getId();
                lastId = bug.getFv().getId() - 1; //l'ultima prima della fv
                for(i=idIv; i<=lastId; i++){

                    for (Release rel : releaseList){
                        Release relAv = null;
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
}
