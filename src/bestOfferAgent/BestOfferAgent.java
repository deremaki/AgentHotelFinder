package bestOfferAgent;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import utils.CSVHelper;
import utils.HotelsResult;

import java.io.IOException;
import java.util.List;

public class BestOfferAgent extends Agent {

    private String bestOffersFileDestination = "C:\\Robot\\BestOffers.csv";

    @Override
    protected void setup() {

        registerToDF();

        addBehaviour(new TickerBehaviour(this, 3000) {
            @Override
            public void onTick() {
                var msg = receive();
                if (msg == null) {
                    return;
                }

                var ontology = msg.getOntology();
                if (ontology.equals("save")) {
                    save(msg);
                } else if (ontology.equals("get")) {
                    var results = CSVHelper.ReadCsv(bestOffersFileDestination);
                    var reply = msg.createReply();
                    HotelsResult hotelsResult = new HotelsResult();
                    hotelsResult.result = results;
                    try {
                        reply.setContentObject(hotelsResult);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    send(reply);
                }
            }
        });
    }

    private void save(ACLMessage msg) {
        try {
            var content = (HotelsResult)msg.getContentObject();
            var city = msg.getConversationId();
            CSVHelper.AppendCsv(content.result, bestOffersFileDestination, msg.getConversationId());
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
    }

    private void registerToDF() {
        ServiceDescription sd = new ServiceDescription();
        sd.setType("BestOffer");
        sd.setName(getLocalName());

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

    }
}
