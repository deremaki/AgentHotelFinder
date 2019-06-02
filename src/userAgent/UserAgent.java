package userAgent;

import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import utils.*;

public class UserAgent extends Agent {

    private String destination = "Warszawa";
    private Date dateFrom = new Date(20,6,2019);
    private Date dateTo = new Date(27,6,2019);
    private double minimumRating = 4.0;

    @Override
    protected void setup() {
        super.setup();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("UserAgent: Setting up User Agent " + getLocalName());
        addBehaviour(new HotelRequestBehaviour(this));
    }

    private class HotelRequestBehaviour extends OneShotBehaviour {

        public HotelRequestBehaviour(Agent a) {
            super(a);
        }

        @Override
        public void action() {

            SequentialBehaviour seq = new SequentialBehaviour();
            addBehaviour(seq);

            //request start
            seq.addSubBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    System.out.print("Enter where you want to stay:");
                    try {
                        destination = br.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.print("Enter start date of your stay (dd/MM/yyyy):");
                    try {
                        dateFrom = new SimpleDateFormat("dd/MM/yyyy").parse(br.readLine());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    System.out.print("Enter end date of your stay (dd/MM/yyyy):");
                    try {
                        dateFrom = new SimpleDateFormat("dd/MM/yyyy").parse(br.readLine());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    System.out.print("Enter minimum rating you are expecting:");
                    try {
                        minimumRating = Double.parseDouble(br.readLine());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    System.out.println("UserAgent: Requesting MainAgent search - destination: " + destination + ", from " + dateFrom.toString() + " to " + dateTo.toString() + " with minimum rating of " + minimumRating);

                    ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
                    message.setContent(destination);
                    message.addReceiver(new AID( "mainAgent", AID.ISLOCALNAME));
                    send(message);
                }
            });

            //wait for best offer
            seq.addSubBehaviour(new myReceiver(myAgent, 100000, MessageTemplate.MatchPerformative(ACLMessage.INFORM)){
                public void handle( ACLMessage msg)
                {
                    if (msg != null ) {
                        if(msg.getPerformative() == ACLMessage.INFORM ) {
                            System.out.println("UserAgent: Result for " + destination);
                            System.out.println(msg.getContent());
                        }
                    }
                }
            });
        }
    }
}
