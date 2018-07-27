# So Gong (2091011) | Mu Lung Dojo Hall

status = -1
dojoHall = 925020001

if sm.getFieldID() == dojoHall:
    def init():
        sm.sendNext("My master is the strongest person in Mu Lung, and YOU wish to challenge HIM? I have a feeling you'll regret this.\r\n#b"
                    "#L0#I want to challenge Mu Lung Dojo.#l\r\n"
                    "#L1#What is Mu Lung Dojo?#l\r\n"
                    "#L2#What rewards can I get from Mu Lung Dojo?#l\r\n"
                    "#L3#How many attempts do I have left today?#l\r\n"
                    "#L4#I'd like to enter the Unity Training Center.#l\r\n")

    def action(response, answer):
        global status, selection
        status += 1

        if status == 0:
            selection = answer

            if selection == 0: # I want to challenge Mu Lung Dojo
                if not sm.getParty() is None:
                    sm.sendSayOkay("Please leave your party before attempting the Mu Lung Dojo")
                    sm.dispose()
                else:
                    sm.warpInstanceIn(925070100) # Dojo Floor 1
                    sm.dispose()

            elif selection == 1: # What is Mu Lung Dojo?
                sm.sendSayOkay("#r//TODO")
                sm.dispose()
            elif selection == 2: # What rewards can I get from Mu Lung Dojo?
                sm.sendSayOkay("#r//TODO")
                sm.dispose()
            elif selection == 3: # How many attempts do I have left today?
                sm.sendSayOkay("#r//TODO")
                sm.dispose()
            elif selection == 4: # I'd like to enter the Unity Training Center.
                sm.sendSayOkay("#r//TODO")
                sm.dispose()

else:
    def init():
        sm.sendAskYesNo("Are you giving up already?")

    def action(response, answer):
        if response == 1:
            sm.warpInstanceOut(dojoHall)
        sm.dispose()