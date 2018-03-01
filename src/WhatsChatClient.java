import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.awt.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.event.*;

//Multicast stuff
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

public class WhatsChatClient extends JFrame {

	MulticastSocket multicastSocket = null;
	MulticastSocket multicastSocketDirectory = null;
	InetAddress multicastGroupDirectory = null;
	InetAddress multicastGroup = null;
	String user = null; //Need validation, no spaces, no starting with numbers
	String userDesc = null; //Used to keep track of user description of this instance, if any
	String userImg = null; //Used to keep track of image of this instance, if any
	String selectedUserP = null; //... Selected user's profile
	String selectedUserDesc = null;//... Selected user's profile desc
	String selectedUserImg = null;//... Selected user's profile img
	ArrayList<String> groupList = null; //Used to keep track of the groups the instance is in
	ArrayList<String> userList = null; //Used to keep list of whoever's online
	Map<String, String> groups = null; //Map with group name, and IP address
	Map<String, ArrayList<String>> groupMembers = null; //Map with group name, list of group members in the group
	Map<String, ArrayList<String>> chatHistory = null; //Map with chat history, by group name


	Random ran = new Random();
	String baseReservedAdd = "230.1.";
	int toReserveX = 2 + ran.nextInt(254);
	int toReserveY = 2 + ran.nextInt(254);

	String broadcastAdd = "230.1.1.1";


	String nameCheckCmd = "n/.";
	String groupCheckCmd = "g/.";
	String groupAddCmd = "a/.";
	String groupRemvCmd = "r/.";
	String separatorTrail = "o/."; //Used to separate names for commands with multiple names
	String groupEditCmd = "e/.";
	String lastMsgReqCmd = "m/.";
	String userListReqCmd = "u/.";
	String lastMsgRecvCmd = "w/.";
	String userListRecvCmd = "n/.";
	String grpListRecvCmd = "q/.";
	String groupDelCmd = "d/.";
	String leavNetCmd = "f/.";
	String profileReqCmd = "p/.";
	String profileRecvCmd = "i/.";

	String groupSeparatorTrail = "g/."; //Used to differentiate between user and group name for chat history
	String userSeparatorTrail = "-"; //Used to differentiate between user names
	String msgSeparatorTrail = "="; //Used to differentiate between chat messages
	String ipSeparatorTrail = "]";
	String profileSeparatorTrail = "::::"; // Used to differentiate profile photo and short description

	DefaultListModel<String> userListModel = null; //use addElement to add into list,
	DefaultListModel<String> groupListModel = null; //list = new JList(listModel) to update Jlist

	int isRegisterGuy = 0; //Used to check if this instance is from the guy who wants to register
	int isCreatingGuy = 0; //Used to check if this instance is from the guy who wants to create a new group
	int isOldestUser = 0; //Used to check if this instance is the oldest guy who joined the instance
	int isProfileAsker = 0; //Used to check if this instance is the guy asking for profile
	Map<String, Integer> isOldestGrpMem = null; //Used to check if this instance is oldest guy of certain groups

	String activeGroup = null; //Use to see which group is active for the instance

	int initAction = 0; //Used to check if this is coming from the instance which initiated the action



	private JPanel contentPane;
	private JTextField textFieldUser;
	private JPanel panelOnlineList;
	private JTextField textFieldGroup;
	private JTextField textFieldTextMsg;
	private JList<String> listUsers;
	private JList<String> listGroups;

	//Profile image variables
	JLabel lblProfilePhoto;
	BufferedImage bufProfile;

	/* Flags and sizes */
	public static int HEADER_SIZE = 8;
	public static int MAX_PACKETS = 255;
	public static int SESSION_START = 128;
	public static int SESSION_END = 64;
	public static int DATAGRAM_MAX_SIZE = 65507 - HEADER_SIZE;
	public static int MAX_SESSION_NUMBER = 255;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WhatsChatClient frame = new WhatsChatClient();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public WhatsChatClient() {

		//Initialize variables
		user = ""; //Need validation, no spaces, no starting with numbers
		userList = new ArrayList<String>(); //Used to keep list of whoever's online
		groupList = new ArrayList<String>(); //Used to keep track of the groups the instance is in
		groups = new HashMap<String, String>(); //Map with group name, and IP address
		groupMembers = new HashMap<String, ArrayList<String>>(); //Map with group name, list of group members in the group
		chatHistory = new HashMap<String, ArrayList<String>>(); //Map with chat history, by group name
		userListModel = new DefaultListModel<String>(); //use addElement to add into list,
		groupListModel = new DefaultListModel<String>(); //list = new JList(listModel) to update Jlist

		userDesc = ""; //Used to keep track of user description of this instance, if any
		userImg = ""; //Used to keep track of image of this instance, if any
		selectedUserP = ""; //... Selected user's profile
		selectedUserDesc = "";//... Selected user's profile desc
		selectedUserImg = "";//... Selected user's profile img


		isOldestGrpMem = new HashMap<String, Integer>(); //Used to check if this instance is oldest guy of certain groups
		activeGroup = ""; //Use to see which group is active for the instance


		try {
			multicastSocket = new MulticastSocket(6789);
		} catch (Exception ex){
			ex.printStackTrace();
		}

		setTitle("WhatsChat");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				sendLeaveMsg();
				super.windowClosing(e);
			}
		});
		setBounds(100, 100, 998, 583);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		//Instruction
		JPanel panelTips = new JPanel();
		panelTips.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Instructions", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panelTips.setBounds(4, 38, 475, 37);
		contentPane.add(panelTips);
		panelTips.setLayout(null);

		JLabel lblToolTip = new JLabel("Please register a name.");
		lblToolTip.setBounds(6, 16, 459, 14);
		panelTips.add(lblToolTip);

		//Profile UI
		JPanel panelProfile = new JPanel();
		panelProfile.setBounds(543, 11, 418, 120);
		panelProfile.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Profile",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		contentPane.add(panelProfile);

		JButton btnUpload = new JButton("Upload");
		btnUpload.setEnabled(false);
		btnUpload.setBounds(238, 91, 78, 23);
		btnUpload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser();
				chooser.showOpenDialog(null);
				File f = chooser.getSelectedFile();
				System.out.println(f.length());
				if (f.length() < (50 * 1024)) {
					try {
						bufProfile = scaleImage(120, 120, ImageIO.read(new File(f.getAbsolutePath())));
						ImageIcon profileIcon = new ImageIcon(bufProfile);// get the image from file chooser and scale it to
						// match JLabel size
						lblProfilePhoto.setIcon(profileIcon);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} else {
					lblToolTip.setText("Please choose a file with a size of less than 50Kb!");
				}
			}
		});

		panelProfile.setLayout(null);
		panelProfile.add(btnUpload);

		lblProfilePhoto = new JLabel("");
		lblProfilePhoto.setHorizontalAlignment(SwingConstants.CENTER);
		lblProfilePhoto.setBounds(277, 11, 86, 69);
		panelProfile.add(lblProfilePhoto);

		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane_1.setBounds(10, 56, 210, 45);
		panelProfile.add(scrollPane_1);

		JTextArea textAreaDescription = new JTextArea();
		textAreaDescription.setLineWrap(true);
		scrollPane_1.setViewportView(textAreaDescription);
		textAreaDescription.setEditable(false);

		JButton btnSave = new JButton("Save");
		btnSave.setEnabled(false);
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (lblProfilePhoto.getIcon() == null || textAreaDescription.getText().toString().equals("")) {
					lblToolTip.setText("Please include both a short description and upload a photo!");
				} else {
					String imageData = encodeToString(bufProfile, "jpg");
					userDesc = textAreaDescription.getText().toString();
					userImg = imageData;
					lblToolTip.setText("User profile saved!");
				}
			}
		});
		btnSave.setBounds(322, 91, 89, 23);
		panelProfile.add(btnSave);

		JLabel lblProfileName = new JLabel("Name:");
		lblProfileName.setBounds(10, 17, 40, 14);
		panelProfile.add(lblProfileName);

		JLabel lblChoosenName = new JLabel("-");
		lblChoosenName.setBounds(60, 17, 106, 14);
		panelProfile.add(lblChoosenName);

		JLabel lblDescription = new JLabel("Description:");
		lblDescription.setBounds(10, 38, 75, 14);
		panelProfile.add(lblDescription);

		JLabel lblUserlist = new JLabel("All");
		lblUserlist.setBounds(109, 206, 116, 14);
		contentPane.add(lblUserlist);
		lblUserlist.setHorizontalAlignment(SwingConstants.RIGHT);

		JButton btnRegister = new JButton("Register User");
		btnRegister.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				initAction = 1;
				if (btnRegister.getText().equals("Register User")){
					if (textFieldUser.getText().equals("")){
						lblToolTip.setText("Please enter a name before trying to register!");
					} else {
						String requestMsg = nameCheckCmd + textFieldUser.getText();
						isRegisterGuy = 1;
						btnRegister.setEnabled(false);
						textFieldUser.setEditable(false);
						lblToolTip.setText("Connecting... Please wait.");
						sendBroadcastData(requestMsg);
					}
				}
			}
		});
		btnRegister.setBounds(10, 11, 144, 23);
		contentPane.add(btnRegister);

		textFieldUser = new JTextField();
		textFieldUser.setBounds(164, 12, 194, 20);
		contentPane.add(textFieldUser);
		textFieldUser.setColumns(10);

		panelOnlineList = new JPanel();
		panelOnlineList.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Online Users", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panelOnlineList.setBounds(10, 217, 219, 266);
		contentPane.add(panelOnlineList);
		panelOnlineList.setLayout(null);

		JScrollPane scrollPaneUsers = new JScrollPane();
		scrollPaneUsers.setBounds(10, 21, 199, 234);
		panelOnlineList.add(scrollPaneUsers);

		listUsers = new JList<String>();
		listUsers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listUsers.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					if(listUsers.getSelectedIndex() > -1){
						if (user.equals(listUsers.getSelectedValue())){
							selectedUserP = "";
							selectedUserDesc = "";
							selectedUserImg = "";

							btnSave.setEnabled(true);
							btnUpload.setEnabled(true);

							updateProfilePanel(textAreaDescription, lblChoosenName, lblProfilePhoto);
						} else {
							isProfileAsker = 1;
							selectedUserP = listUsers.getSelectedValue();
							String requestMessage = profileReqCmd + selectedUserP;
							sendBroadcastData(requestMessage);
						}
					}
				}
			}
		});
		scrollPaneUsers.setViewportView(listUsers);

		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Group Management", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panel.setBounds(4, 79, 475, 67);
		contentPane.add(panel);
		panel.setLayout(null);

		JButton btnGroupCreate = new JButton("Create");
		btnGroupCreate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!user.equals("")) {
					if (btnGroupCreate.getText().equals("Create")) {
						if (textFieldGroup.getText().equals("")) {
							lblToolTip.setText("Please enter a group name first!");
						} else {

							//Append a new address for the group
							String addressForNewGroup = baseReservedAdd + String.valueOf(toReserveX)
									+ "." + String.valueOf(toReserveY);
							//Refresh X and Y IP address parts incase of new creation
							toReserveX = 2 + ran.nextInt(254);
							toReserveY = 2 + ran.nextInt(254);

							String requestMsg = groupCheckCmd + textFieldGroup.getText() + separatorTrail + user + groupSeparatorTrail + addressForNewGroup;
							isCreatingGuy = 1;
							lblToolTip.setText("Connecting... Please wait.");
							sendBroadcastData(requestMsg);
						}
					} else {
						if (listUsers.getSelectedIndex() == -1) {
							lblToolTip.setText("Please select the member you want to add through the list first!");
						} else {
							String temp = listUsers.getSelectedValue();
							if (temp.equals(user)) {
								lblToolTip.setText("Please do not try to add yourself!");
							} else {
								String requestMessage = groupAddCmd + activeGroup + separatorTrail + temp;
								lblToolTip.setText("Member added!");
								sendBroadcastData(requestMessage);
							}
						}
					}
				} else {
					lblToolTip.setText("Please register for a user name first!");
				}
			}
		});
		btnGroupCreate.setBounds(30, 33, 97, 23);
		btnGroupCreate.setEnabled(false);
		panel.add(btnGroupCreate);

		JButton btnGroupDelete = new JButton("Delete");
		btnGroupDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!user.equals("")) {
					if (btnGroupDelete.getText().equals("Delete")) {
						if (textFieldGroup.getText().equals("")) {
							lblToolTip.setText("Please don't try to delete with an empty name");
						} else {
							String requestMessage = groupDelCmd + textFieldGroup.getText();
							lblToolTip.setText("Connecting... Please wait.");
							sendBroadcastData(requestMessage);
						}

					} else {
						if (listUsers.getSelectedIndex() == -1) {
							lblToolTip.setText("Please select the member you want to remove through the list first!");
						} else {
							String temp = listUsers.getSelectedValue();
//							if (temp.equals(user)) {
//								lblToolTip.setText("Please do not try to remove yourself!");
//							} else {
							String requestMessage = groupRemvCmd + activeGroup + separatorTrail + temp;
							lblToolTip.setText("Member removed!");
							sendBroadcastData(requestMessage);
//							}
						}
					}
				}
			}
		});
		btnGroupDelete.setBounds(163, 33, 97, 23);
		btnGroupDelete.setEnabled(false);
		panel.add(btnGroupDelete);

		JButton btnGroupEdit = new JButton("Edit Group Name");
		btnGroupEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!user.equals("")) {
					if (btnGroupEdit.getText().equals("Edit Group Name")) {
						if (textFieldGroup.getText().equals("") || textFieldGroup.getText().equals(activeGroup)) {
							lblToolTip.setText("Please don't try to edit with an empty name or the same name!");
						} else {
							String requestMessage = groupEditCmd + activeGroup + separatorTrail + textFieldGroup.getText();
							lblToolTip.setText("Connecting... Please wait.");
							sendBroadcastData(requestMessage);
						}
					} else {
						toggleUserListUI(lblUserlist);
					}
				}
			}
		});
		btnGroupEdit.setBounds(293, 33, 172, 23);
		btnGroupEdit.setEnabled(false);
		panel.add(btnGroupEdit);

		JPanel panelGroups = new JPanel();
		panelGroups.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Groups", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panelGroups.setBounds(260, 217, 219, 266);
		contentPane.add(panelGroups);
		panelGroups.setLayout(null);

		JScrollPane scrollPaneGroups = new JScrollPane();
		scrollPaneGroups.setBounds(10, 21, 199, 234);
		panelGroups.add(scrollPaneGroups);

		JPanel panelChat = new JPanel();
		panelChat.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Chat Box", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panelChat.setBounds(543, 135, 430, 355);
		contentPane.add(panelChat);
		panelChat.setLayout(null);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBounds(10, 21, 410, 323);
		panelChat.add(scrollPane);

		JTextArea textArea = new JTextArea();
		textArea.setLineWrap(true);
		scrollPane.setViewportView(textArea);
		textArea.setEditable(false);

		JPanel panelChosenGrp = new JPanel();
		panelChosenGrp.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Group Name", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panelChosenGrp.setBounds(4, 158, 475, 43);
		contentPane.add(panelChosenGrp);
		panelChosenGrp.setLayout(null);

		textFieldGroup = new JTextField();
		textFieldGroup.setBounds(6, 16, 360, 20);
		panelChosenGrp.add(textFieldGroup);
		textFieldGroup.setColumns(10);

		JButton btnUnselect = new JButton("Unselect");
		btnUnselect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Group Management", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
				btnGroupCreate.setText("Create");
				btnGroupDelete.setText("Delete");
				btnGroupEdit.setText("Edit Group Name");

				btnUnselect.setEnabled(false);
			}
		});
		btnUnselect.setBounds(376, 15, 89, 23);
		btnUnselect.setEnabled(false);
		panelChosenGrp.add(btnUnselect);

		listGroups = new JList<String>();
		listGroups.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					if (listGroups.getSelectedIndex() > -1) {
						String groupName = listGroups.getSelectedValue();
						String check = activeGroup + " - Active";
						if (!check.equals(groupName)) {
							swapActiveGroupUI(activeGroup, groupName);
							activeGroup = groupName;
							textFieldGroup.setText(groupName);

							updateUserUIList(lblUserlist);
							refreshTextArea(textArea);
						} else {
							textFieldGroup.setText(activeGroup);
						}

						panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Member Management", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
						btnGroupCreate.setText("Add");
						btnGroupDelete.setText("Remove");
						btnGroupEdit.setText("Toggle user list");

						btnUnselect.setEnabled(true);
					}
				}
			}
		});
		scrollPaneGroups.setViewportView(listGroups);
		listGroups.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);



		JButton btnSend = new JButton("Send");
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (textFieldTextMsg.getText().equals("")) {
					lblToolTip.setText("Enter some text before trying to send!");
				} else if (activeGroup.equals("")) {
					lblToolTip.setText("Please have an active group before trying to send!");
				} else {
					String messageToSend = activeGroup + groupSeparatorTrail + user + ": " + textFieldTextMsg.getText();
					sendData(messageToSend);
					textFieldTextMsg.setText(null);
					textFieldTextMsg.requestFocusInWindow();
				}
			}
		});
		btnSend.setBounds(884, 512, 89, 23);
		contentPane.add(btnSend);

		textFieldTextMsg = new JTextField();
		textFieldTextMsg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (textFieldTextMsg.getText().equals("")) {
					lblToolTip.setText("Enter some text before trying to send!");
				} else if (activeGroup.equals("")) {
					lblToolTip.setText("Please have an active group before trying to send!");
				} else {
					String messageToSend = activeGroup + groupSeparatorTrail + user + ": " + textFieldTextMsg.getText();
					sendData(messageToSend);
					textFieldTextMsg.setText(null);
					textFieldTextMsg.requestFocusInWindow();
				}
			}
		});
		textFieldTextMsg.setBounds(10, 513, 864, 20);
		contentPane.add(textFieldTextMsg);
		textFieldTextMsg.setColumns(10);



		//Initiate link to broadcast multicast group
		try{
			multicastSocketDirectory = new MulticastSocket(6799);
			multicastGroupDirectory = InetAddress.getByName(broadcastAdd);
			multicastSocketDirectory.joinGroup(multicastGroupDirectory);

			//Start thread for broadcast side
			new Thread(new Runnable() {
				@Override
				public void run() {
					byte buf[] = new byte[DATAGRAM_MAX_SIZE];
					DatagramPacket dgpRecieved
							= new DatagramPacket(buf, buf.length);


					//Request for list of users
					String requestMessage = userListReqCmd;
					sendBroadcastData(requestMessage);

					while(true) {
						try{
							multicastSocketDirectory.receive(dgpRecieved);
							byte[] receivedData = dgpRecieved.getData();
							int length = dgpRecieved.getLength();
							String receivedMessage = new String(receivedData, 0, length);
							//Start decoding the message
							//Message for registering user
//							if (initAction == 1) {
							if (receivedMessage.substring(0, 3).equals(nameCheckCmd)){
								String userCheck = receivedMessage.substring(3);
								//Check name against the current table, also validate at the same time
								if (runUserCheck(userCheck) == true){
									if (isAlphaNumeric(userCheck)) {
										if (isRegisterGuy == 1) {
											oldestUserCheck();
										}
										//Update table nonetheless for other clients
										userList.add(userCheck);
										updateUserUIList(lblUserlist);
										if (isRegisterGuy == 1) {
											//Update self name
											user = userCheck;
//											btnRegister.setEnabled(true);
//											btnRegister.setText("Disconnect");
											lblToolTip.setText("Name has been registered! Create a group or wait for an invite!");

											btnGroupCreate.setEnabled(true);
											btnGroupDelete.setEnabled(true);
											btnGroupEdit.setEnabled(true);

											//Enable profiling
											selectedUserP = "";
											selectedUserDesc = "";
											selectedUserImg = "";

											btnSave.setEnabled(true);
											btnUpload.setEnabled(true);

											updateProfilePanel(textAreaDescription, lblChoosenName, lblProfilePhoto);

											isRegisterGuy = 0;
										}
									} else {
										lblToolTip.setText("Please only include alphanumeric characters in your name!");
									}
								} else {
									//Give a notice that name has been taken already
									if(isRegisterGuy == 1) {
										lblToolTip.setText("Username is invalid, please choose another name.");
										textFieldUser.setEditable(true);
										btnRegister.setEnabled(true);

										isRegisterGuy = 0;
									}
								}
							}

							//Message for creating group
							if (receivedMessage.substring(0, 3).equals(groupCheckCmd)){
								String groupCreateCheck = receivedMessage.substring(3);
								String groupName = groupCreateCheck.substring(0, groupCreateCheck.indexOf(separatorTrail));
								String name = groupCreateCheck.substring(groupCreateCheck.indexOf(separatorTrail) + 3, groupCreateCheck.indexOf(groupSeparatorTrail));
								String ipAdd = groupCreateCheck.substring(groupCreateCheck.indexOf(groupSeparatorTrail) + 3);
								//Check group name against current table.
								if (!runGroupNameCheck(groupName)){
									if(isAlphaNumeric(groupName)) {
										if (isCreatingGuy == 1) {
											//Join the group
											ipAdd = checkGroupIP(ipAdd);
											joinGroup(ipAdd, textArea, groupName);
											groupList.add(groupName);
											updateGroupUIList();

											swapActiveGroupUI(activeGroup, groupName);
											activeGroup = groupName;

											lblToolTip.setText("Group created!");

											panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Member Management", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
											btnGroupCreate.setText("Add");
											btnGroupDelete.setText("Remove");
											btnGroupEdit.setText("Toggle user list");

											btnUnselect.setEnabled(true);

											//Instance becomes oldest member of group
											isOldestGrpMem.put(groupName, 1);

											refreshTextArea(textArea);

											isCreatingGuy = 0;
										}
										//Update table nonetheless for other clients
										groups.put(groupName, ipAdd);
										ArrayList<String> tempMembers = new ArrayList<String>();
										if (groupMembers.get(groupName) != null) {
											tempMembers = groupMembers.get(groupName);
											tempMembers.add(name);
											groupMembers.put(groupName, tempMembers);
										} else {
											tempMembers.add(name);
											groupMembers.put(groupName, tempMembers);
										}
									} else {
										lblToolTip.setText("Please only include alphanumeric characters in your group name!");
									}
								} else {
									//Give a notice that name has been taken already
									if (isCreatingGuy == 1) {
										lblToolTip.setText("Group name has already been taken...");
										isCreatingGuy = 0;
									}
								}
							}

							//Message for adding users into a group
							if (receivedMessage.substring(0, 3).equals(groupAddCmd)){
								String addToGroupMessage = receivedMessage.substring(3);
								//Split up group name and user name
								String addToGroup = addToGroupMessage.substring(0, addToGroupMessage.indexOf(separatorTrail));
								String userToAdd = addToGroupMessage.substring(addToGroupMessage.indexOf(separatorTrail) + 3);
								//Check if user already belongs to the group
								if (!runMemberCheck(addToGroup, userToAdd)){
									//User doesn't belong in the group

									//The user being added will try to join the group stated
									if (user.equals(userToAdd)){
										String ipAdd = groups.get(addToGroup);
										joinGroup(ipAdd, textArea, addToGroup);
										groupList.add(addToGroup);
										textFieldGroup.setText(addToGroup);
										swapActiveGroupUI(activeGroup, addToGroup);
										activeGroup = addToGroup;

										panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Member Management", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
										btnGroupCreate.setText("Add");
										btnGroupDelete.setText("Remove");
										btnGroupEdit.setText("Toggle user list");

										btnUnselect.setEnabled(true);

										refreshTextArea(textArea);
										updateUserUIList(lblUserlist);

										lblToolTip.setText("You have been added to the group: " + addToGroup);

										//Request for chat history if this instance doesn't have it
										if (chatHistory.get(addToGroup) == null || chatHistory.get(addToGroup).size() < 2){
											String requestChatMessage = lastMsgReqCmd + addToGroup + separatorTrail + user;
											sendBroadcastData(requestChatMessage);
										}
									}

									//Update the group members to include the new guy
									ArrayList<String> tempMembers = new ArrayList<String>();
									if (groupMembers.get(addToGroup) != null){
										tempMembers = groupMembers.get(addToGroup);
										tempMembers.add(userToAdd);
										groupMembers.put(addToGroup, tempMembers);
									} else {
										tempMembers.add(userToAdd);
										groupMembers.put(addToGroup, tempMembers);
									}

									updateGroupUIList();
								} else {
									lblToolTip.setText("User is already in the group!");
								}
							}

							//Message for removing users from a group
							if (receivedMessage.substring(0, 3).equals(groupRemvCmd)){
								String remvFrmGroupMessage = receivedMessage.substring(3);
								//Split up group name and user name
								String remvFrmGroup = remvFrmGroupMessage.substring(0,  remvFrmGroupMessage.indexOf(separatorTrail));
								String userToRemv = remvFrmGroupMessage.substring(remvFrmGroupMessage.indexOf(separatorTrail) + 3);
								//Check if user belongs to the group
								if (runMemberCheck(remvFrmGroup, userToRemv)){
									//User belongs to group

									//User in question will leave the group stated
									if (user.equals(userToRemv)){
										leaveGroup(remvFrmGroup);
//											refreshTextArea(textArea);
										updateGroupUIList();
										lblToolTip.setText("You have been removed from the group: " + remvFrmGroup);
										leaveGroupPt2(remvFrmGroup);
									} else {
										lblToolTip.setText(userToRemv + " has been removed from the group: " + remvFrmGroup);
									}

									//Update the group members to remove the new guy
									ArrayList<String> tempMembers = new ArrayList<String>();
									if (groupMembers.get(remvFrmGroup) != null){
										tempMembers = groupMembers.get(remvFrmGroup);
										tempMembers.remove(userToRemv);
										groupMembers.put(remvFrmGroup, tempMembers);
									}

									if (lblUserlist.getText().equals("Current Group")){
										userListModel = new DefaultListModel<String>();
										ArrayList<String> tempList = groupMembers.get(activeGroup);
										for (String user : tempList){
											userListModel.addElement(user);
										}
										listUsers.setModel(userListModel);
									}

									//Find the next oldest member of group
									if (user.equals(groupMembers.get(remvFrmGroup).get(0))){
										isOldestGrpMem.put(remvFrmGroup, 1);
									}

									updateUserUIList(lblUserlist);
								}
							}

							//Message for editing a group name
							if (receivedMessage.substring(0, 3).equals(groupEditCmd)){
								String editGroupMessage = receivedMessage.substring(3);
								//Split up old group name and new group name
								String oldGroupName = editGroupMessage.substring(0, editGroupMessage.indexOf(separatorTrail));
								String newGroupName = editGroupMessage.substring(editGroupMessage.indexOf(separatorTrail) + 3);

								//Check if new group name already exists
								if (!runGroupNameCheck(newGroupName)){
									if (isAlphaNumeric(newGroupName)) {
										//Tell everyone to update the group name

										//Update groups hashmap and remove old details
										if (groups.get(oldGroupName) != null) {
											String tempIPAdd = groups.get(oldGroupName);
											groups.put(newGroupName, tempIPAdd);
											groups.remove(oldGroupName);
										}
										//Update group members hashmap and remove old details
										if (groupMembers.get(oldGroupName) != null) {
											ArrayList<String> tempMemList = groupMembers.get(oldGroupName);
											groupMembers.put(newGroupName, tempMemList);
											groupMembers.remove(oldGroupName);
										}
										//Update group conversation list hashmap and remove old details
										if (chatHistory.get(oldGroupName) != null) {
											ArrayList<String> tempConvoHist = chatHistory.get(oldGroupName);
											chatHistory.put(newGroupName, tempConvoHist);
											chatHistory.remove(oldGroupName);
										}

										//Update local group list
										if (activeGroup.equals(oldGroupName)) {
											groupList.set(groupList.indexOf(oldGroupName + " - Active"), newGroupName + " - Active");
											activeGroup = newGroupName;
										} else {
											groupList.set(groupList.indexOf(oldGroupName), newGroupName);
										}

										updateGroupUIList();
										lblToolTip.setText(oldGroupName + "changed name to: " + newGroupName);
									} else {
										lblToolTip.setText("Please only include alphanumeric chracters in your new group name!");
									}
								} else {
									//Inform user that they cannot use the new name
									lblToolTip.setText("Group name already taken, please choose another.");
								}
							}

							//Message for deleting a group
							if (receivedMessage.substring(0, 3).equals(groupDelCmd)){
								String delGroupMsg = receivedMessage.substring(3);

								//Check if group exists
								if (!runGroupNameCheck(delGroupMsg)){
									lblToolTip.setText("Group doesn't exist, can't delete!");
								} else {
									//Group exists, prepare to delete everything related to the group

									//Leave the group first
									leaveGroup(delGroupMsg);

									//Remove stuff related to the group - Group list, group array, group members, group chat history
									if (activeGroup.equals(delGroupMsg)){
										groupList.remove(delGroupMsg + " - Active");
										//Clear text area screen
										textArea.setText(null);
									} else {
										groupList.remove(delGroupMsg);
									}
									groups.remove(delGroupMsg);
									groupMembers.remove(delGroupMsg);
									chatHistory.remove(delGroupMsg);

									//Refresh UI
									lblToolTip.setText("The group: " + delGroupMsg + "has been deleted!");
									updateGroupUIList();
									updateUserUIList(lblUserlist);

									leaveGroupPt2(delGroupMsg);
								}
							}
//
//								initAction = 0;
//							}

							//Message for requesting the last chat history of a group
							if (receivedMessage.substring(0, 3).equals(lastMsgReqCmd)){
								//Shouldn't have errors as everything is back end...
								String lastMsgReqMessage = receivedMessage.substring(3);
								//Split up group name and user name who requested it
								String groupNameReq = lastMsgReqMessage.substring(0, lastMsgReqMessage.indexOf(separatorTrail));
								String requestor = lastMsgReqMessage.substring(lastMsgReqMessage.indexOf(separatorTrail) + 3);

								//Check if this instance even has an instance of this variable
								if (isOldestGrpMem.get(groupNameReq) != null){
									if (isOldestGrpMem.get(groupNameReq) == 1){
										replyChatHistory(requestor, groupNameReq);
									}
								}
							}

							//Message for requesting for list of login users
							if (receivedMessage.substring(0, 3).equals(userListReqCmd)){

								//Check if this instance is the oldest user in the network
								if (isOldestUser == 1){
									replyUserList();
									replyGroupList();
								}
							}

							//Message for receiving chat history of group
							if (receivedMessage.substring(0, 3).equals(lastMsgRecvCmd)){
								String msgRecvMessage = receivedMessage.substring(3);
								//split up user name to receive and chat message
								String userToRecv = msgRecvMessage.substring(0, msgRecvMessage.indexOf(separatorTrail));
								String groupName = msgRecvMessage.substring((msgRecvMessage.indexOf(separatorTrail) + 3), msgRecvMessage.indexOf(groupSeparatorTrail));

								//Check if user is the one to receive the message history
								if(user.equals(userToRecv)){
									//Start decoding message history
									addChatHistory(groupName, msgRecvMessage.substring(msgRecvMessage.indexOf(groupSeparatorTrail) + 3));
									refreshTextArea(textArea);
								}
							}

							//Message for receiving user list of network
							if(receivedMessage.substring(0, 3).equals(userListRecvCmd)){
								String msgUserRecvMessage = receivedMessage.substring(3);
								//Gives the updated list to anyone with an empty user list
								if(userList.size() == 0){
									//Start decoding the list of users
									addUserList(msgUserRecvMessage, lblUserlist);
								}
							}

							//Message for receiving group list on startup
							if(receivedMessage.substring(0, 3).equals(grpListRecvCmd)){
								String msgGrpRecvMessage = receivedMessage.substring(3);
								//Gives the updated list to anyone with an empty user list
								if(groups.size() < 1){
									//Start decoding the list of users
									addGrpList(msgGrpRecvMessage);
								}
							}

							//Message for updating user lists when someone leaves
							if(receivedMessage.substring(0, 3).equals(leavNetCmd)){
								String leavNetMessage = receivedMessage.substring(3);
								String userToRemv = leavNetMessage.substring(0, leavNetMessage.indexOf(separatorTrail));
								String groupsToRemv = leavNetMessage.substring(leavNetMessage.indexOf(separatorTrail) + 3);

								updateLeaving(userToRemv, groupsToRemv, lblUserlist);
							}

							//Message for when someone requests for a profile picture
							if(receivedMessage.substring(0, 3).equals(profileReqCmd)){
								String profReqMessage = receivedMessage.substring(3);
								//Check if this instance is the asker's requested guy
								if (user.equals(profReqMessage)){
									//Reply own details
									String replyMessage = profileRecvCmd;
									if (userDesc.equals("")){
										replyMessage += "none";
									} else {
										replyMessage += userDesc + separatorTrail + userImg;
									}

									sendBroadcastData(replyMessage);
								}
							}

							//Message for when someone replies for a profile picture
							if(receivedMessage.substring(0,3).equals(profileRecvCmd)){
								String profRecvMessage = receivedMessage.substring(3);
								//Check if this instance is the requestor
								if (isProfileAsker == 1){
									if(profRecvMessage.equals("none")){
										selectedUserDesc = "";
										selectedUserImg = "";
									} else {
										selectedUserDesc = profRecvMessage.substring(0, profRecvMessage.indexOf(separatorTrail));
										selectedUserImg = profRecvMessage.substring(profRecvMessage.indexOf(separatorTrail) + 3);

									}

									textAreaDescription.setEditable(false);
									btnSave.setEnabled(false);
									btnUpload.setEnabled(false);
									updateProfilePanel(textAreaDescription, lblChoosenName, lblProfilePhoto);
									isProfileAsker = 0;
								}
							}

						} catch (IOException ex){
							ex.printStackTrace();
						}
					}
				}
			}).start();

			//Create a new thread to keep listening for packets from the group
			new Thread(new Runnable() {
				@Override
				public void run() {
					byte buf[] = new byte[1000];
					DatagramPacket dgpRecieved
							= new DatagramPacket(buf, buf.length);
					while(true) {
						if (multicastSocket!= null) {
							try {
								multicastSocket.receive(dgpRecieved);
								byte[] recievedData = dgpRecieved.getData();
								int length = dgpRecieved.getLength();
								//Assumed we received string
								if (length > 0) {
									String msg = new String(recievedData, 0, length);
									String group = msg.substring(0, msg.indexOf(groupSeparatorTrail));
									String messageText = msg.substring(msg.indexOf(groupSeparatorTrail) + 3);

									updateChatHistory(group, messageText);
									refreshTextArea(textArea);
								}
							} catch (IOException ex) {
								ex.printStackTrace();
							}
						}
					}
				}
			}).start();

		} catch (Exception ex){
			ex.printStackTrace();
		}
	}

	//Space for magic functions
	//Check if username is valid, can't have spaces, can't start with numbers, can't be longer than 8 characters
	private boolean runUserCheck(String username){
		if (username.length() > 8 || username.contains(" ") || Character.isDigit(username.charAt(0))){
		} else {
			if (!userList.contains(username)){
				return true;
			}
		}
		return false;
	}

	//Check if group name already exists
	private boolean runGroupNameCheck(String groupName){
		if (groups.containsKey(groupName)){
			return true;
		} else {
			return false;
		}
	}

	//Joins a group according to an IP address, adds a thread with every group joined
	private void joinGroup(String groupIP, JTextArea textArea, String groupName){
		try {
			multicastGroup = InetAddress.getByName(groupIP);
			//Join
			multicastSocket.joinGroup(multicastGroup);
			//Send a joined message
			String message =groupName + groupSeparatorTrail + user + " joined";
			sendData(message);


		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	//Check if member already exists in a group
	private boolean runMemberCheck(String groupName, String userName){
		ArrayList<String> tempList = new ArrayList<String>();
		if (groupMembers.get(groupName) != null){
			tempList = groupMembers.get(groupName);
			if (tempList.contains(userName)){
				return true;
			}
		}
		return false;
	}

	//Lets the instance leave the specified group
	private void leaveGroup(String groupName){
		try{
			String groupAdd = groups.get(groupName);

			sendData(groupAdd + groupSeparatorTrail + user + " has left the group.");

			//Clear group details
			//chatHistory.remove(groupName);
			if (activeGroup.equals(groupName)){
				groupList.remove(groupName + " - Active");

				if (groupList.size() > 0){
					activeGroup = groupList.get(0);
					String temp = activeGroup + " - Active";
					groupList.set(0, temp);
				} else {
					activeGroup = "";
				}
			} else {
				groupList.remove(groupName);
			}
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}

	//Isolated leaveGroup method due to bug
	private void leaveGroupPt2(String groupName){
		try {
			String groupAdd = groups.get(groupName);
			multicastGroup = InetAddress.getByName(groupAdd);
			multicastSocket.leaveGroup(multicastGroup);
		} catch (Exception ex){
			ex.printStackTrace();
		}

	}

	//Sends out the chat history, up to 10 last messages
	private void replyChatHistory(String requestor, String groupName){
		int limiter;
		String returnMsg = lastMsgRecvCmd + requestor + separatorTrail + groupName + groupSeparatorTrail;
		if (chatHistory.get(groupName) != null){
			ArrayList<String> tempList = chatHistory.get(groupName);
			if (tempList.size() > 11){
				limiter = 10;
			} else {
				limiter = tempList.size();
			}

			for (int i = limiter; i > 0; i--){
				returnMsg += tempList.get(tempList.size() - i) + msgSeparatorTrail + " ";
			}
		} else {
			returnMsg += "none";
		}

		sendBroadcastData(returnMsg);
	}

	//Oldest guy should broadcast out his list of logged in users
	private void replyUserList(){
		String returnMsg = userListRecvCmd;
		if (userList.size() != 0){
			for (int i = 0; i < userList.size(); i++){
				returnMsg += userList.get(i) + userSeparatorTrail + " ";
			}
		} else {
			returnMsg += "none";
		}

		sendBroadcastData(returnMsg);
	}

	//Oldest guy broadcasts out his groups
	private void replyGroupList(){
		String returnMsg = grpListRecvCmd;
		if (groups.size() > 0){
			for (Map.Entry<String, String> entry : groups.entrySet()){

				String name = entry.getKey();
				String ipAdd = entry.getValue();

				ArrayList<String> userList = groupMembers.get(name);
				String users = "";

				for (String userName : userList){
					users += userName + msgSeparatorTrail + " ";
				}

				returnMsg += name + groupSeparatorTrail + ipAdd + ipSeparatorTrail + users;

				returnMsg += userSeparatorTrail;

			}

			sendBroadcastData(returnMsg);
		}
	}

	//Adds in the chat history from the oldest group member
	private void addChatHistory(String groupname, String chathist){
		if (!chathist.equals("none")){
			String[] list = chathist.split(msgSeparatorTrail);
			ArrayList<String> tempList = new ArrayList<String>(Arrays.asList(list));
			for (int i = 0; i < tempList.size(); i++){
				String temp  = tempList.get(i).trim();
				tempList.set(i, temp);
			}
			tempList.remove(tempList.size() - 1);
			chatHistory.put(groupname, tempList);
		}
	}

	//Adds in user list from oldest network user
	private void addUserList(String userString, JLabel lblUserlist){
		if (!userString.equals("none")){
			String[] list = userString.split(userSeparatorTrail);
			userList = new ArrayList<String>(Arrays.asList(list));
			for (int i = 0; i < userList.size(); i++){
				String temp  = userList.get(i).trim();
				userList.set(i, temp);
			}
			userList.remove(userList.size()-1);
		}
		updateUserUIList(lblUserlist);
	}

	//Adds in group list from oldest network user
	private void addGrpList(String groupString){
		if (!groupString.equals("none")){
			String[] list = groupString.split(userSeparatorTrail);
			ArrayList<String> tempGroups = new ArrayList<String>(Arrays.asList(list));
			for (int i = 0; i < tempGroups.size(); i++){
				String temp  = tempGroups.get(i).trim();
				tempGroups.set(i, temp);
			}
//			if (tempGroups.size() > 1) {
//				tempGroups.remove(tempGroups.size() - 1);
//			}

			String[] list2;
			String groupName;
			String groupIP;
			String members;
			ArrayList<String> tempMembers;
			//Get users in groups
			for (String pack : tempGroups){
				groupName = pack.substring(0, pack.indexOf(groupSeparatorTrail));
				groupIP = pack.substring(pack.indexOf(groupSeparatorTrail) + 3, pack.indexOf(ipSeparatorTrail));
				members = pack.substring(pack.indexOf(ipSeparatorTrail) + 1);
				list2 = members.split(msgSeparatorTrail);

				tempMembers = new ArrayList<String>(Arrays.asList(list2));

				for (int i = 0; i < tempMembers.size(); i++){
					String temp  = tempMembers.get(i).trim();
					tempMembers.set(i, temp);
				}
//				if (tempMembers.size() > 1) {
//					tempMembers.remove(tempMembers.size() - 1);
//				}
				groups.put(groupName, groupIP);
				groupMembers.put(groupName, tempMembers);
			}
			updateGroupUIList();
		}
	}

	//Sends data towards the groups
	private void sendData(String message){
		try{
			byte[] buf = message.getBytes();
			DatagramPacket dgpSend = new DatagramPacket(buf, buf.length, multicastGroup, 6789);
			multicastSocket.send(dgpSend);
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}
	//sendBroadcastData
	private void sendBroadcastData(String message){
		try{
			byte[] buf = message.getBytes();
			DatagramPacket dgpSend = new DatagramPacket(buf, buf.length, multicastGroupDirectory, 6799);
			multicastSocketDirectory.send(dgpSend);
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	//Updates chat history recieved from the oldest member of the group
	private void updateChatHistory(String groupname, String messageText){
		ArrayList<String> tempList = new ArrayList<String>();
		//Check if a chat history log already exists, if yes, update it
		if (chatHistory.get(groupname) != null){
			tempList = chatHistory.get(groupname);
			tempList.add(messageText);
			chatHistory.put(groupname, tempList);
		} else {
			//If chat history log doesn't exist, add it in here
			tempList.add(messageText);
			chatHistory.put(groupname, tempList);
		}
	}

	private void updateUserUIList(JLabel lblUserList){

		if (lblUserList.getText().equals("Active Group")){
			userListModel = new DefaultListModel<String>();
			ArrayList<String> tempList = groupMembers.get(activeGroup);
			for (String user : tempList){
				userListModel.addElement(user);
			}
			listUsers.setModel(userListModel);
		} else {
			userListModel = new DefaultListModel<String>();
			for (String user : userList){
				userListModel.addElement(user);
			}
			listUsers.setModel(userListModel);
		}
	}

	private void updateGroupUIList(){
		groupListModel = new DefaultListModel<String>();
		for (String groupname : groupList){
			groupListModel.addElement(groupname);
		}
		listGroups.setModel(groupListModel);
	}

	private void oldestUserCheck(){
		if (userList.size() == 0){
			isOldestUser = 1;
		}
	}

	private void swapActiveGroupUI(String oldAct, String newAct){
		if (oldAct.equals("")){
			String tempName = groupList.get(groupList.indexOf(newAct));
			int tempInd = groupList.indexOf(newAct);
			tempName = tempName + " - Active";
			groupList.set(tempInd, tempName);
		} else {
			String tempNameOld = groupList.get(groupList.indexOf(oldAct + " - Active"));
			int tempIndOld = groupList.indexOf(oldAct + " - Active");
			tempNameOld = tempNameOld.substring(0, oldAct.length());
			groupList.set(tempIndOld, tempNameOld);

			String tempName = groupList.get(groupList.indexOf(newAct));
			int tempInd = groupList.indexOf(newAct);
			tempName = tempName + " - Active";
			groupList.set(tempInd, tempName);
		}

		updateGroupUIList();
	}

	private void toggleUserListUI(JLabel lblUserList){
		if (lblUserList.getText().equals("All")){
			userListModel = new DefaultListModel<String>();
			ArrayList<String> tempList = groupMembers.get(activeGroup);
			for (String user : tempList){
				userListModel.addElement(user);
			}
			lblUserList.setText("Active Group");
			listUsers.setModel(userListModel);
		} else {
			lblUserList.setText("All");
			updateUserUIList(lblUserList);
		}
	}

	public void refreshTextArea(JTextArea textArea){
		textArea.setText(null);
		if (chatHistory.get(activeGroup) != null) {
			for (String text : chatHistory.get(activeGroup)) {
				textArea.append(text + "\n");
			}
		}
	}

	//When someone leaves the network, everyone is to update their lists that contain that user
	public void updateLeaving(String userName, String groupNames, JLabel lblUserList){
		//userName variable refers to the name of the guy who left
		//groupNames will be the leaving guy's list of groups that he's joined

		//Remove the guy from other online people's lists
		userList.remove(userName);

		//Check if self becomes oldest member in the network
		if (!user.equals("")) {
			if (userList != null || user != null || user.equals(userList.get(0))) {
				isOldestUser = 1;
			}
		}

		//Remove the guy from other online people's groupmember lists

		//Gather all the group names the leaver belongs to
		if (!groupNames.equals("none")){
			String[] temp = groupNames.split(msgSeparatorTrail);
			ArrayList<String> tempGroups = new ArrayList<String>(Arrays.asList(temp));
			for (int i = 0; i < tempGroups.size(); i++){
				String tempName  = tempGroups.get(i).trim();
				tempGroups.set(i, tempName);
			}
			tempGroups.remove(tempGroups.size()-1);

			//Remove the leaver from the groups
			for (String groupName : tempGroups){
				if(groupMembers.get(groupName) != null){
					ArrayList<String> names = groupMembers.get(groupName);
					names.remove(userName);

					//Check if self becomes the oldest member of the group
					if (!user.equals("")) {
						if (names.size() > 0) {
							if (user.equals(names.get(0))) {
								isOldestGrpMem.put(groupName, 1);
							}
						}
					}
					//Update groupMembers
					groupMembers.put(groupName, names);
				}
			}
		}

		//Update UI lists
		if (lblUserList.getText().equals("Current Group")){
			userListModel = new DefaultListModel<String>();
			ArrayList<String> tempList = groupMembers.get(activeGroup);
			for (String user : tempList){
				userListModel.addElement(user);
			}
			listUsers.setModel(userListModel);
		} else {
			updateUserUIList(lblUserList);
		}
		updateGroupUIList();
	}

	//Send disconnect message
	public void sendLeaveMsg(){
		if (!user.equals("")){
			//Prepare message to send
			String requestMessage = leavNetCmd + user + separatorTrail;

			//Generate groups string
			String groupNames = "";
			if (groupList.size() > 0) {
				for (String groupName : groupList) {
					if (groupName.length() > 9 && activeGroup.equals(groupName.substring(0, groupName.length() - 9))){
						groupNames += groupName.substring(0, groupName.length() - 9) + msgSeparatorTrail + " ";
					} else {
						groupNames += groupName + msgSeparatorTrail + " ";
					}
				}
			} else {
				groupNames = "none";
			}
			requestMessage += groupNames;

			sendBroadcastData(requestMessage);
		}
	}

	//Check for conflicting group IP Address
	public String checkGroupIP(String ipAdd){
		while(groups.containsValue(ipAdd)){
			//Append a new address for the group
			ipAdd = baseReservedAdd + String.valueOf(toReserveX)
					+ "." + String.valueOf(toReserveY);
			//Refresh X and Y IP address parts incase of new creation
			toReserveX = 2 + ran.nextInt(254);
			toReserveY = 2 + ran.nextInt(254);
		}

		return ipAdd;
	}




	//Codes for user profiling

	//Rescale image
	public static BufferedImage scaleImage(int w, int h, BufferedImage img) throws Exception {
		BufferedImage bi;
		bi = new BufferedImage(w, h, BufferedImage.TRANSLUCENT);
		Graphics2D g2d = (Graphics2D) bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
		g2d.drawImage(img, 0, 0, w, h, null);
		g2d.dispose();
		return bi;
	}

	//Convert image to bytes
	public static byte[] bufferedImageToByteArray(BufferedImage image, String format) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, format, baos);
		return baos.toByteArray();
	}


	//Convert image to string codes
	public static String encodeToString(BufferedImage image, String type) {
		String imageString = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			ImageIO.write(image, type, bos);
			byte[] imageBytes = bos.toByteArray();

			BASE64Encoder encoder = new BASE64Encoder();
			imageString = encoder.encode(imageBytes);

			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return imageString;
	}

	//Convert string code into image
	public static BufferedImage decodeToImage(String imageString) {

		BufferedImage image = null;
		byte[] imageByte;
		try {
			BASE64Decoder decoder = new BASE64Decoder();
			imageByte = decoder.decodeBuffer(imageString);
			ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
			image = ImageIO.read(bis);
			bis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return image;
	}

	public void updateProfilePanel(JTextArea textAreaDesc, JLabel userName, JLabel profilePic){
		if (!selectedUserP.equals("")){
			//Selected a user
			textAreaDesc.setEditable(false);
			userName.setText(selectedUserP);
			if (!selectedUserDesc.equals("")){
				textAreaDesc.setText(selectedUserDesc);
				BufferedImage selectedProfImgBuf = decodeToImage(selectedUserImg);
				ImageIcon selectProfImg = new ImageIcon(selectedProfImgBuf);
				profilePic.setText("");
				profilePic.setIcon(selectProfImg);
			} else {
				profilePic.setText("No picture");
				profilePic.setIcon(null);
				textAreaDesc.setText("User does not have a profile setup!");
			}
		} else {
			textAreaDesc.setEditable(true);
			userName.setText(user);
			if (!userDesc.equals("")){
				textAreaDesc.setText(userDesc);
				BufferedImage profImgBuf = decodeToImage(userImg);
				ImageIcon profImg = new ImageIcon(profImgBuf);
				profilePic.setText("");
				profilePic.setIcon(profImg);
			} else {
				textAreaDesc.setText(null);
				profilePic.setIcon(null);
				profilePic.setText("No picture");
			}
		}
	}


	public boolean isAlphaNumeric(String s){
		String pattern= "^[a-zA-Z0-9]*$";
		return s.matches(pattern);
	}
}
