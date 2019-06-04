package mainAgent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

import utils.*;

public class MainAgent extends Agent {

    private LinkedList<AID> hotelRobots;
    private LinkedList<AID> bestOfferAgents;
    private List<List<String>> bookingResultsFromAllAgents;
    private Map<String, Boolean> responseFromRobot;
    private String destination = "C:\\Robot\\FinalResult.csv";
    private String city;
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
                    city = msg.getContent().split(";")[0];
                    myAgent.addBehaviour(new SearchForHotelsBehaviour(msg));
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, 5000) {
            @Override
            public void onTick() {
                if (responseFromRobot.values().stream().anyMatch((v) -> v)) {
                    CSVHelper.WriteCsc(bookingResultsFromAllAgents, destination);
                    // save to best offer
                    ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
                    message.setConversationId(city);
                    message.setOntology("save");
                    message.addReceiver(bestOfferAgents.getFirst());
                    HotelsResult hotelsResult = new HotelsResult();
                    hotelsResult.result = bookingResultsFromAllAgents;
                    try {
                        message.setContentObject(hotelsResult);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    send(message);
                    // end program
                    myAgent.doDelete();
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

            hotelRobots = getAgents("HotelRobot");
            bestOfferAgents = getAgents("BestOffer");

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

            for (AID agent: bestOfferAgents) {

                responseFromRobot.put(agent.getName(), false);

                addSubBehaviour(new OneShotBehaviour() {
                    @Override
                    public void action() {
                        System.out.println("MainAgent: send request to " + agent);
                        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
                        var city = requestMessage.getContent().split(";")[0];
                        message.setContent(city);
                        message.setOntology("get");
                        message.addReceiver(agent);
                        send(message);
                    }
                });

                addSubBehaviour(new myReceiver(myAgent, 100000, MessageTemplate.MatchPerformative( ACLMessage.INFORM )) {
                    public void handle( ACLMessage msg) {
                        if (msg != null ) {
                            System.out.println("MainAgent: " + agent + " found " + msg.getContent());
                            HotelsResult result = null;
                            try {
                                result = (HotelsResult) msg.getContentObject();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            responseFromRobot.put(msg.getSender().getName(), true);
                            if (result != null) {
                                if (bookingResultsFromAllAgents.size() > 0 && result.result.size() > 0) {
                                    result.result.remove(0);
                                }
                                result.result.add(Arrays.asList("From best offer"));
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

    private LinkedList<AID> getAgents(String name) {
        LinkedList<AID> sellers = null;

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd  = new ServiceDescription();
        sd.setType(name);
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
