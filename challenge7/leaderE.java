

private static final int SPOT_TABLE_DEPTH = 7;

private static final int RING_FORMATION_MSG = 0;
private static final int NEED_MASTER_MSG = 1;
private static final int BE_MY_FOLLOWER_MSG = 2;
private static final int ACCEPT_FOLLOWER_MSG = 3;
private static final int REJECT_FOLLOWER_MSG = 4;
private static final int IM_ALL_SET_MSG = 5;
private static final int LEADER_ANN_MSG = 6;


private static final int SPOT_NOT_ACTIVE_THRESHOLD = 2000;
private static final int SPOT_RING_REFORM_PERIOD = 5000;
private static final boolean OTHER_REQUEST = true;
private static final boolean SELF_CHECK = false;


private long[] spotAddr = new long[SPOT_TABLE_DEPTH];
private int[] spotNotActive = new int [SPOT_TABLE_DEPTH];
private boolean[] spotNotActiveFlag = new boolean[SPOT_TABLE_DEPTH];
private boolean[] spotBeMyFollowerReqSentFlag = new boolean[SPOT_TABLE_DEPTH];
private boolean[] thatSpotNeedsMaster = new boolean[SPOT_TABLE_DEPTH];
private boolean[] spotReadyFlag = new boolean[SPOT_TABLE_DEPTH];
private boolean iAmLeader=false;
private long master;//master addr
private long leader;
private int masterIndex;
private	boolean beMyFollowerMsgSent = false;
private boolean isRingComplete = false;

private long myAddr;
private long lastInitTime=0;



void checkRingFormation(boolean selfCheckOrOtherRequest)
{
	long currentTime = System.currentTimeMillis();
	boolean reform = false;
	
	for(int ii=0;ii<spotAddr.length;ii++)
	{
		if((spotNotActiveFlag[i]==true && spotAddr[i]!=0) || lastInitTime==0 || master == 0)
		{
			reform = true;
			break;
		}
	}
//            try {

//		if(currentTime-lastCheckMasterTime>=SPOT_RING_REFORM_PERIOD)
		if( ( reform==true || selfCheckOrOtherRequest == OTHER_REQUEST) && currentTime - lastInitTime > SPOT_REFORM_PERIOD)
		{
			initializeInfo();
			sendRingFormationRequest();
			sendNeedMasterRequest();
			lastInitTime = currentTime;
		}
//                    } catch (TimeoutException tex) {        
//                    		return false;
//					}

		if(reform)
		{
			leds.getLED(0).setColor(red);
			leds.getLED(0).setOn;
		}
		if(master!=0 && follower !=0)
		{
			sendImAllSetMsg();
			leds.getLED(0).setOff;
		}
		

}	


void initializeInfo()
{
	master=0;
	follower=0;
	masterIndex=0;
	beMyFollowerMsgSent=false;
	iAmLeader=false;
	leader=0;
	
	for(int i =0; i<spotAddr.length;i++)
	{


		spotAddr[i]=0;
		spotNotActive[i]=0;
		spotNotActiveFlag[i]=false;
		spotBeMyFollowerReqSentFlag[i]=false;		
		thatSpotNeedsMaster[i] = false;
		spotReadyFlag[i]=false;
	}

}

int updateSpotCount(long newSrcAddr)
{
    int i;

    int empty=0;
	int spotNum;
	
    int newAddrFlag=true;

	for(i=0;i<spotAddr.length;i++)//go through the current addr table. compare
	{
		if(newSrcAddr==spotAddr[i])
		{
			for(int j=0;j<spotAddr.length;j++)
			{
				if(j!=i || spotAddr[j]==0)spotNotActive[j] ++;//not active counter
				if(j==i)spotNotActive[j] =0;//sender's notActive counter clear
				if(spotNotActive[j]>SPOT_NOT_ACTIVE_THRESHOLD) spotNotActiveFlag[j]=True;empty=j;//the spot is not active ATM
				else spotNotActiveFlag[j]=False;
			}
			newAddrFlag=false;
			break;
		}
	}
	
    if(newAddrFlag==true)
    {
		spotAddr[empty]=newSrcAddr;
    }
    return spotNum;
}


void parseMsg(Radiogram rdg)//used after a packet is received. 
{

	updateSpotCount(rdg.getAddressAsLong());
	int msgType = rdg.readInt();
		
	switch(msgType)
	{
		case RING_FORMATION_MSG:
			
			checkRingFormation(OTHER_REQUEST);
			break;

		case NEED_MASTER_MSG:   

			checkFollowerStatus(rdg.getAddressAsLong());break;

		case BE_MY_FOLLOWER_MSG:

			checkFollowerRequest(rdg.getAddressAsLong());break;

		case ACCEPT_FOLLOWER_MSG:

			followerAccept(rdg.getAddressAsLong);break;			
			
		case REJECT_FOLLOWER_MSG:
		
			followerReject(rdg.getAddressAsLong);break;
			
		case IM_ALL_SET_MSG:
		
			tryLeaderElection(rdg.getAddressAsLong);break;	

		case LEADER_ANN_MSG:
		
			updateMyLeader(rdg.getAddressAsLong);break;
												
	}
}





//transmitting

boolean void sendRingFormationRequest()
{
	            try {
				        RadiogramConnection txConn = null;
                txConn = (RadiogramConnection)Connector.open("radiogram://broadcast:" + BROADCAST_PORT);
                txConn.setMaxBroadcastHops(1);      // don't want packets being rebroadcasted
                Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());
                leds.getLED(1).setColor(green);
                leds.getLED(1).setOn();


                    xdg.reset();
						xdg.writeInt(RING_FORMATION_MSG);
                    txConn.send(xdg);
                 leds.getLED(1).setOff();
					return true;
            } catch (IOException ex) {
                return false;
            }
}


boolean sendNeedMasterRequest()
{
		try {
					RadiogramConnection txConn = null;
	                txConn = (RadiogramConnection)Connector.open("radiogram://broadcast:" + BROADCAST_PORT);
    	            txConn.setMaxBroadcastHops(1);      // packets are only broadcast once
        	        Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());
            	    leds.getLED(1).setColor(green);
                	leds.getLED(1).setOn();

				    xdg.reset();
					xdg.writeInt(NEED_MASTER_MSG);
	                txConn.send(xdg);
    	            leds.getLED(1).setOff();
					return true;
            } catch (IOException ex) {
                return false;
            }
	
}

boolean sendBeMyFollwerRequest(long addressToBeSend)
{
	String targetIEEEAddress = IEEEAddress.toDottedHex(addressToBeSend);
	
		try {

					RadiogramConnection txConn = null;
                txConn = (RadiogramConnection)Connector.open("radiogram://"+ targetIEEEAddress + ":" + BROADCAST_PORT);
                Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());

                leds.getLED(1).setColor(green);
                leds.getLED(1).setOn();

				   xdg.reset();
					xdg.writeInt(BE_MY_FOLLOWER_MSG);
                 txConn.send(xdg);
                 leds.getLED(1).setOff();
					return true;
            } catch (IOException ex) {
                return false;
            }

}

boolean sendAcceptFollowerRequest(long addressToBeSend)
{
	String targetIEEEAddress = IEEEAddress.toDottedHex(addressToBeSend);
	
		try {

					RadiogramConnection txConn = null;
                txConn = (RadiogramConnection)Connector.open("radiogram://"+ targetIEEEAddress + ":" + BROADCAST_PORT);
                Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());

                leds.getLED(1).setColor(green);
                leds.getLED(1).setOn();

				   xdg.reset();
					xdg.writeInt(ACCEPT_FOLLOWER_MSG);
                 txConn.send(xdg);
                 leds.getLED(1).setOff();
					return true;
            } catch (IOException ex) {
                return false;
            }

}

boolean sendRejectFollowerRequest(long addressToBeSend)
{
	String targetIEEEAddress = IEEEAddress.toDottedHex(addressToBeSend);
	
		try {

					RadiogramConnection txConn = null;
                txConn = (RadiogramConnection)Connector.open("radiogram://"+ targetIEEEAddress + ":" + BROADCAST_PORT);
                Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());

                leds.getLED(1).setColor(green);
                leds.getLED(1).setOn();

				   xdg.reset();
					xdg.writeInt(REJECT_FOLLOWER_MSG);
                 txConn.send(xdg);
                 leds.getLED(1).setOff();
					return true;
            } catch (IOException ex) {
                return false;
            }

}



boolean sendImAllSetMsg()
{
		try {
					RadiogramConnection txConn = null;
	                txConn = (RadiogramConnection)Connector.open("radiogram://broadcast:" + BROADCAST_PORT);
    	            txConn.setMaxBroadcastHops(1);      // packets are only broadcast once
        	        Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());
            	    leds.getLED(1).setColor(green);
                	leds.getLED(1).setOn();

				    xdg.reset();
					xdg.writeInt(IM_ALL_SET_MSG);
	                txConn.send(xdg);
    	            leds.getLED(1).setOff();
					return true;
            } catch (IOException ex) {
                return false;
            }
	
}

void sendLeaderAnnouncement()
{
		try {
					RadiogramConnection txConn = null;
	                txConn = (RadiogramConnection)Connector.open("radiogram://broadcast:" + BROADCAST_PORT);
    	            txConn.setMaxBroadcastHops(2);      // packets are only broadcast twice
        	        Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());
            	    leds.getLED(1).setColor(blue);
                	leds.getLED(1).setOn();

				    xdg.reset();
					xdg.writeInt(LEADER_ANN_MSG);
	                txConn.send(xdg);
					return true;
            } catch (IOException ex) {
                return false;
            }
	
}


//receiving


boolean checkFollowerStatus(long srcAddress)
{

	if(follower!=0)return -1;
	else
	{
		for(int jjj=0;jjj<spotAddr.length;jjj++)
		{
			if(spotAddr==srcAddress)thatSpotNeedsMaster[jjj]=true;
		}
		
		if(beMyFollowerMsgSent==false)
		{

			for(int i=0;i<spotAddr.length;i++)
			{
				if(spotBeMyFollowerReqSentFlag[i]==false)
				{
					sendBeMyFollwerRequest(spotAddr[i]);
					spotBeMyFollowerReqSentFlag[i]=true;
					beMyFollowerMsgSent=true;
					return true;
				}
				
			}
			
		}
		return false;
	}

}


boolean checkFollowerStatus()
{

	if(follower!=0)return -1;
	else
	{
		if(beMyFollowerMsgSent==false)
		{

			for(int i=0;i<spotAddr.length;i++)
			{
				if(spotBeMyFollowerReqSentFlag[i]==false && thatSpotNeedsMaster[i]==true)
				{
					sendBeMyFollwerRequest(spotAddr[i]);
					spotBeMyFollowerReqSentFlag[i]=true;
					beMyFollowerMsgSent=true;
					return true;
				}
				
			}
			
		}
		return false;
	}

}

boolean checkFollowerRequest(long srcAddress)
{
	if(master!=0)
	{
		sendRejectFollowerRequest(srcAddress);
		return false;
	}
	else
	{
		sendAcceptFollowerRequest(srcAddress);
		master = srcAddress;
		for(int i=0;i<spotAddr.length;i++)
		{
			if(spotAddr[i]==master){masterIndex=i;break;}
		}
		return true;
	}

}

boolean followerAccept(long srcAddress)
{

	follower = srcAddress;
	for(int i=0;i<spotBeMyFollowerReqSentFlag.length;i++)
	{
		spotBeMyFollowerReqSentFlag[i]=false;
	}
	beMyFollowerMsgSent=false;

}

boolean followerReject(long srcAddress)
{
	beMyFollowerMsgSent = false;
	for(int aaa = 0; aaa<spotAddr.length;aaa++)
	{
		if(spotAddr[aaa]==srcAddress)thatSpotNeedsMaster[aaa]=false;
	}
	checkFollowerStatus();

}

boolean updateMyLeader(long srcAddress)
{
	if(srcAddress>myAddr){ leader=srcAddress;return true;}
	else if(srcAddress<myAddr)
		{
			sendLeaderAnnouncement();
			iAmLeader=true;
			leader=myAddr;
		}
		return false;
	
}




boolean tryLeaderElection(long readySpotAddr)
{
	if(leader!=0)return true;

	boolean iMightBeLeader=false;
	boolean allSet = true;
    
    for(int n=0;n<spotAddr.length;n++)
    {
    	if(spotAddr[n]==readySpotAddr)spotReadyFlag[n]=true;
		if(spotReadyFlag[n]==false&&spotAddr[n]!=0)allSet=false;
	}
	if(allSet==false)return false;
	else {
    	iMightBeLeader=true;
		for(int aa=0;aa<spotAddr.length;aa++)
		{
    		if(spotAddr[n]>myAddr)iMightBeLeader=false;
		}
    }
    iAmLeader=iMightBeLeader;
	if(iAmLeader)
	{
		sendLeaderAnnouncement();
		leader=myAddr;
	}

}
