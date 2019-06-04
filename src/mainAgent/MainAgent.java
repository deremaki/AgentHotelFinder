package mainAgent;

import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.File;
import java.util.*;

import jade.lang.acl.UnreadableException;
import utils.*;

public class MainAgent extends Agent {

    private LinkedList<AID> hotelRobots;
    private List<List<String>> bookingResultsFromAllAgents;
    private Map<String, Boolean> responseFromRobot;
    private String destination = "C:\\Robot\\FinalResult.csv";

    @Override
    protected void setup() {
        super.setup();

        Object[] args = getArguments();
        bookingResultsFromAllAgents = new ArrayList<>();
        responseFromRobot = new HashMap<>();

        File dir = new File("C:/Robot/");
        for(File file: dir.listFiles())
            if (!file.isDirectory())
                file.delete();

        System.out.println("MainAgent: Hello, my name is " + getLocalName());

        addBehaviour(new myReceiver(this, 60000,  MessageTemplate.MatchPerformative(ACLMessage.REQUEST)) {
            public void handle( ACLMessage msg)
            {
                if (msg != null ) {
                    myAgent.addBehaviour(new SearchForHotelsBehaviour(msg));
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, 5000) {
            @Override
            public void onTick() {
                if (responseFromRobot.values().stream().anyMatch((v) -> v)) {
                    CSVHelper.WriteCsc(bookingResultsFromAllAgents, destination);
                }
            }
        });
    }

    private class SearchForHotelsBehaviour extends SequentialBehaviour {

        private ACLMessage requestMessage;
        private boolean takeIt;
        private int currentBestPrice = 100000;
        private AID currentBestSeller = new AID();

        public SearchForHotelsBehaviour(ACLMessage msg) {

            super();
            requestMessage = msg;
        }

        @Override
        public void onStart() {
            System.out.println("MainAgent: Looking for hotels: " + requestMessage.getContent());

            hotelRobots = getHotelRobots();

            currentBestPrice = 100000;

            for (AID robot: hotelRobots) {

                responseFromRobot.put(robot.getName(), false);

                addSubBehaviour(new OneShotBehaviour() {
                    @Override
                    public void action() {
                        System.out.println("MainAgent: send request to " + robot);
                        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
                        message.setContent(requestMessage.getContent());
                        message.addReceiver(robot);
                        send(message);
                    }
                });

                addSubBehaviour(new myReceiver(myAgent, 150000, MessageTemplate.MatchPerformative( ACLMessage.INFORM )) {
                    public void handle( ACLMessage msg) {
                        if (msg != null ) {
                            System.out.println("MainAgent: " + robot + " found " + msg.getContent());
                            int offer = 0;//Integer.parseInt( msg.getContent());
                            HotelsResult result = null;
                            try {
                                result = (HotelsResult) msg.getContentObject();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (offer < currentBestPrice) {
                                currentBestPrice = offer;
                            }
                            responseFromRobot.put(msg.getSender().getName(), true);
                            if (result != null) {
                                if (bookingResultsFromAllAgents.size() > 0 && result.result.size() > 0) {
                                    result.result.remove(0);
                                }
                                bookingResultsFromAllAgents.addAll(result.result);
                            }
                        }
                    }
                });
            }

            //report best
            addSubBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    System.out.println("MainAgent: Stay in " + requestMessage.getContent() + " for only " + currentBestPrice + " was found");
                    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                    message.setContent(requestMessage.getContent() + " for " + currentBestPrice);
                    message.addReceiver(new AID( "userAgent", AID.ISLOCALNAME));
                    send(message);
                }
            });
        }
    }

    private LinkedList<AID> getHotelRobots() {
        LinkedList<AID> sellers = null;

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd  = new ServiceDescription();
        sd.setType("HotelRobot");
        dfd.addServices(sd);

        try {
            DFAgentDescription[] results = DFService.search(this, dfd);
            sellers = new LinkedList<AID>();
            for (int i = 0; i < results.length; i++) {
                sellers.add(results[i].getName());
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        return sellers;
    }
}
