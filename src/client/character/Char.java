package client.character;

import client.Client;
import client.character.items.*;
import client.character.skills.Core;
import client.character.skills.Skill;
import connection.OutPacket;
import constants.ItemConstants;
import constants.JobConstants;
import constants.SkillConstants;
import enums.DBChar;
import enums.InvType;
import org.hibernate.Session;
import org.hibernate.Transaction;
import server.Server;
import util.FileTime;
import loaders.ItemData;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created on 11/17/2017.
 */
@Entity
@Table(name = "characters")
public class Char {

    @Transient
    private Client client;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "accId")
    private int accId;

    @JoinColumn(name = "equippedInventory")
    @OneToOne
    private Inventory equippedInventory = new Inventory(Inventory.Type.EQUIPPED, 20);

    @JoinColumn(name = "equipInventory")
    @OneToOne
    private Inventory equipInventory = new Inventory(Inventory.Type.EQUIP, 20);

    @JoinColumn(name = "consumeInventory")
    @OneToOne
    private Inventory consumeInventory = new Inventory(Inventory.Type.CONSUME, 20);

    @JoinColumn(name = "etcInventory")
    @OneToOne
    private Inventory etcInventory = new Inventory(Inventory.Type.ETC, 20);

    @JoinColumn(name = "installInventory")
    @OneToOne
    private Inventory installInventory = new Inventory(Inventory.Type.INSTALL, 20);

    @JoinColumn(name = "cashInventory")
    @OneToOne
    private Inventory cashInventory = new Inventory(Inventory.Type.CASH, 32);

    @JoinColumn(name = "avatarData")
    @OneToOne
    private AvatarData avatarData;

    @Transient
    private Ranking ranking;
    @Transient
    private int combatOrders;
    @Transient
    private List<Skill> stolenSkills;
    @Transient
    private List<Skill> chosenSkills;
    @Transient
    private QuestManager questManager;
    @Transient
    private List<ItemPot> itemPots;
    @Transient
    private List<Pet> pets;
    @Transient
    private List<FriendRecord> friends;
    @Transient
    private long money;
    @Transient
    private List<ExpConsumeItem> expConsumeItems;
    @Transient
    private List<MonsterBattleMobInfo> monsterBattleMobInfos;
    @Transient
    private MonsterBattleLadder monsterBattleLadder;
    @Transient
    private MonsterBattleRankInfo monsterBattleRankInfo;
    @Column(name = "fieldID")
    private int fieldID;

    public Char() {
        this(0, "", 0, 0, 0, (short) 0, (byte) -1, (byte) -1, new int[]{});
    }

    public Char(int accId, String name, int keySettingType, int eventNewCharSaleJob, int job, short curSelectedSubJob,
                byte gender, byte skin, int[] items) {
        this.accId = accId;
        avatarData = new AvatarData();
        avatarData.setAvatarLook(new AvatarLook());
        AvatarLook avatarLook = avatarData.getAvatarLook();
        avatarLook.setGender(gender);
        avatarLook.setSkin(skin);
        avatarLook.setFace(items.length > 0 ? items[0] : 0);
        avatarLook.setHair(items.length > 1 ? items[1] : 0);
        this.id = 1;
        List<Integer> hairEquips = new ArrayList<>();
        for(int itemId : items) {
            Equip equip = ItemData.getEquipDeepCopyFromId(itemId);
            if(equip != null) {
                hairEquips.add(itemId);
                if(equip.getiSlot().equals("Wp")) {
                    if(!equip.isCash()) {
                        avatarLook.setWeaponId(itemId);
                    } else {
                        avatarLook.setWeaponStickerId(itemId);
                    }
                }
//                equip.setBagIndex(-ItemConstants.getBodyPartFromItem(equip.getItemId(), getAvatarData().getAvatarLook().getGender()));
//                addItemToInventory(Inventory.Type.EQUIPPED, equip);
            }
        }
        avatarLook.setHairEquips(hairEquips);
        CharacterStat characterStat = new CharacterStat(name, job);
        characterStat.setLevel(1);
        characterStat.setStr(4);
        characterStat.setDex(4);
        characterStat.setInt(4);
        characterStat.setLuk(4);
        characterStat.setHp(50);
        characterStat.setMaxHp(50);
        characterStat.setMp(50);
        characterStat.setMaxMp(50);
        characterStat.setGender(gender);
        characterStat.setSkin(skin);
        characterStat.setFace(items.length > 0 ? items[0] : 0);
        characterStat.setHair(items.length > 1 ? items[1] : 0);
        characterStat.setPosMap(100000000);
        getAvatarData().setCharacterStat(characterStat);
        ranking = new Ranking();
        pets = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            pets.add(new Pet());
        }
        stolenSkills = new ArrayList<>();
        chosenSkills = new ArrayList<>();
        questManager = new QuestManager();
        itemPots = new ArrayList<>();
        friends = new ArrayList<>();
        expConsumeItems = new ArrayList<>();
//        monsterBattleMobInfos = new ArrayList<>();
//        monsterBattleLadder = new MonsterBattleLadder();
//        monsterBattleRankInfo = new MonsterBattleRankInfo();

    }

    public void updateDB() {
        Session session = Server.getInstance().getNewDatabaseSession();
        Transaction tx = session.beginTransaction();
        getAvatarData().updateDB(session, tx);
        for (Inventory inventory : getInventories()) {
            inventory.updateDB(session, tx);
        }
        session.update(this);
        tx.commit();
        session.close();
    }

    public void createInDB() {
        Session session = Server.getInstance().getNewDatabaseSession();
        Transaction tx = session.beginTransaction();
        getAvatarData().createInDB(session, tx);
        for (Inventory inventory : getInventories()) {
            inventory.createInDB(session, tx);
        }
        session.save(this);
        tx.commit();
        session.close();
    }

    public void deleteFromDB() {
        Session session = Server.getInstance().getNewDatabaseSession();
        Transaction tx = session.beginTransaction();
        getAvatarData().deleteFromDB(session, tx);
        for (Inventory inventory : getInventories()) {
            inventory.deleteInDB(session, tx);
        }
        session.delete(this);
        tx.commit();
        session.close();
    }

    public static Char getFromDBById(int userId) {
        Char chr;
        Session session = Server.getInstance().getNewDatabaseSession();
        Transaction transaction = session.beginTransaction();
        chr = session.get(Char.class, userId);
        transaction.commit();
        session.close();
        return chr;
    }

    public AvatarData getAvatarData() {
        return avatarData;
}

    public Ranking getRanking() {
        return ranking;
    }

    public int getId() {
        return id;
    }

    public void setAvatarData(AvatarData avatarData) {
        this.avatarData = avatarData;
    }

    public void setRanking(Ranking ranking) {
        this.ranking = ranking;
    }

    public void setId(int id) {
        this.id = id;
        getAvatarData().getCharacterStat().setCharacterId(id);
        getAvatarData().getCharacterStat().setCharacterIdForLog(id);
    }

    public int getAccId() {
        return accId;
    }

    public void setAccId(int accId) {
        this.accId = accId;
    }

    public Inventory getEquippedInventory() {
        return equippedInventory;
    }

    public void addItemToInventory(Inventory.Type type, Item item) {
        Inventory inventory = getInventoryByType(type);
        if(inventory != null) {
            item.setInventoryId(inventory.getId());
            inventory.addItem(item);
        }
    }

    public Inventory getInventoryByType(Inventory.Type type) {
        switch(type) {
            case EQUIPPED:
                return getEquippedInventory();
            case EQUIP:
                return getEquipInventory();
            case CONSUME:
                return getConsumeInventory();
            case ETC:
                return getEtcInventory();
            case INSTALL:
                return getInstallInventory();
            case CASH:
                return getCashInventory();
            default:
                return null;
        }
    }

    public void setEquippedInventory(Inventory equippedInventory) {
        this.equippedInventory = equippedInventory;
    }

    public Inventory getEquipInventory() {
        return equipInventory;
    }

    public void setEquipInventory(Inventory equipInventory) {
        this.equipInventory = equipInventory;
    }

    public Inventory getConsumeInventory() {
        return consumeInventory;
    }

    public void setConsumeInventory(Inventory consumeInventory) {
        this.consumeInventory = consumeInventory;
    }

    public Inventory getEtcInventory() {
        return etcInventory;
    }

    public void setEtcInventory(Inventory etcInventory) {
        this.etcInventory = etcInventory;
    }

    public Inventory getInstallInventory() {
        return installInventory;
    }

    public void setInstallInventory(Inventory installInventory) {
        this.installInventory = installInventory;
    }

    public Inventory getCashInventory() {
        return cashInventory;
    }

    public void setCashInventory(Inventory cashInventory) {
        this.cashInventory = cashInventory;
    }

    public void encode(OutPacket outPacket, DBChar mask) {
        // CharacterData::Decode
        outPacket.encodeLong(mask.get());
        outPacket.encodeByte(getCombatOrders());
        for (Pet pet : getPets()) {
            outPacket.encodeInt(pet.getActiveSkillCoolTime());
        }
        outPacket.encodeByte(0); // unk, not in kmst
        byte sizeByte = 0;
        outPacket.encodeByte(sizeByte);
        for (int i = 0; i < sizeByte; i++) {
            outPacket.encodeInt(0);
        }

        int sizee = 0;
        outPacket.encodeInt(sizee);
        for (int i = 0; i < sizee; i++) {
            outPacket.encodeInt(0); // nKey
            outPacket.encodeLong(0); // pInfo
        }
        outPacket.encodeByte(0); // again unsure
        //outPacket.encodeBytes(Util.getByteArrayByString("01 00 00 00 01 00 00 00 00 00 00 00 61 64 61 64 00 00 00 00 00 00 00 00 00 00 01 B1 4F 00 00 31 75 00 00 FF 00 00 01 B8 0B 0C 00 05 00 04 00 04 00 32 00 00 00 32 00 00 00 32 00 00 00 32 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 C0 EE 7D 37 00 00 00 00 00 00 00 00 4A D7 3A 78 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 0A 00 00 00 00 05 06 00 00 00 00 00 FF FF FF FF FE FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 84 7A D3 01 20 16 2B 70 00 14 01 04 00 61 64 61 64 01 04 00 61 64 61 64 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 20 20 20 20 3C 00 40 E0 FD 3B 37 4F 01 00 05 00 01 45 06 10 00 00 00 80 05 BB 46 E6 17 02 FF FF FF FF 00 00 00 00 1C 00 00 00 FF 82 06 10 7B 60 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF FF FF FF FF 00 40 E0 FD 3B 37 4F 01 FF FF FF FF 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 07 00 01 A6 5B 10 00 00 00 80 05 BB 46 E6 17 02 FF FF FF FF 00 00 00 00 1C 00 00 00 FF 82 06 10 7B 60 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF FF FF FF FF 00 40 E0 FD 3B 37 4F 01 FF FF FF FF 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 01 F0 DD 13 00 00 00 80 05 BB 46 E6 17 02 FF FF FF FF 00 00 00 00 1C 00 00 00 FF 82 06 10 7B 60 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF FF FF FF FF 00 40 E0 FD 3B 37 4F 01 FF FF FF FF 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 37 00 01 20 E2 11 00 00 00 80 05 BB 46 E6 17 02 FF FF FF FF 3C 00 00 00 01 00 01 00 01 00 01 00 1C 00 00 00 FF 83 06 10 7B 60 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF FF FF FF FF 00 40 E0 FD 3B 37 4F 01 FF FF FF FF 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 01 00 00 00 00 01 09 00 5F 38 00 00 B2 7A D3 01 2C 3B 00 00 B2 7A D3 01 FA 49 00 00 B2 7A D3 01 A5 81 00 00 B2 7A D3 01 A6 81 00 00 B2 7A D3 01 DB 81 00 00 B2 7A D3 01 F1 81 00 00 B2 7A D3 01 F3 81 00 00 B2 7A D3 01 B2 99 00 00 B2 7A D3 01 00 00 00 00 00 00 00 00 FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*

        if (mask.isInMask(DBChar.Character)) {
            getAvatarData().getCharacterStat().encode(outPacket);
            //outPacket.encodeBytes(Util.getByteArrayByString("14 01 04 00 61 64 61 64 01 04 00 61 64 61 64 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 20 20 20 20 3C 00 40 E0 FD 3B 37 4F 01 00 05 00 01 45 06 10 00 00 00 80 05 BB 46 E6 17 02 FF FF FF FF 00 00 00 00 1C 00 00 00 FF 82 06 10 7B 60 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF FF FF FF FF 00 40 E0 FD 3B 37 4F 01 FF FF FF FF 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 07 00 01 A6 5B 10 00 00 00 80 05 BB 46 E6 17 02 FF FF FF FF 00 00 00 00 1C 00 00 00 FF 82 06 10 7B 60 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF FF FF FF FF 00 40 E0 FD 3B 37 4F 01 FF FF FF FF 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 01 F0 DD 13 00 00 00 80 05 BB 46 E6 17 02 FF FF FF FF 00 00 00 00 1C 00 00 00 FF 82 06 10 7B 60 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF FF FF FF FF 00 40 E0 FD 3B 37 4F 01 FF FF FF FF 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 37 00 01 20 E2 11 00 00 00 80 05 BB 46 E6 17 02 FF FF FF FF 3C 00 00 00 01 00 01 00 01 00 01 00 1C 00 00 00 FF 83 06 10 7B 60 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF FF FF FF FF 00 40 E0 FD 3B 37 4F 01 FF FF FF FF 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 01 00 00 00 00 01 09 00 5F 38 00 00 B2 7A D3 01 2C 3B 00 00 B2 7A D3 01 FA 49 00 00 B2 7A D3 01 A5 81 00 00 B2 7A D3 01 A6 81 00 00 B2 7A D3 01 DB 81 00 00 B2 7A D3 01 F1 81 00 00 B2 7A D3 01 F3 81 00 00 B2 7A D3 01 B2 99 00 00 B2 7A D3 01 00 00 00 00 00 00 00 00 FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));

            outPacket.encodeByte(getFriends().size());
            boolean hasBlessingOfFairy = getBlessingOfFairy() != null;
            outPacket.encodeByte(hasBlessingOfFairy);
            if (hasBlessingOfFairy) {
                outPacket.encodeString(getBlessingOfFairy());
            }
            boolean hasBlessingOfEmpress = getBlessingOfEmpress() != null;
            outPacket.encodeByte(hasBlessingOfEmpress);
            if (hasBlessingOfEmpress) {
                outPacket.encodeString(getBlessingOfEmpress());
            }
            outPacket.encodeByte(false); // ultimate explorer, deprecated
        }
        if (mask.isInMask(DBChar.Money)) {
            outPacket.encodeLong(getMoney());
        }
        if (mask.isInMask(DBChar.ItemSlotConsume) || mask.isInMask(DBChar.ExpConsumeItem)) {
            outPacket.encodeInt(getExpConsumeItems().size());
            for (ExpConsumeItem eci : getExpConsumeItems()) {
                eci.encode(outPacket);
            }
        }

        if (mask.isInMask(DBChar.ItemSlotConsume) || mask.isInMask(DBChar.ShopBuyLimit)) {
            int size = 0;
            outPacket.encodeInt(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeLong(0);
                outPacket.encodeLong(0);
            }
        }
        if (mask.isInMask(DBChar.MonsterBattleInfo)) {
            int count = 0; // MonsterBattle_MobInfo
            outPacket.encodeInt(count);
            if(getMonsterBattleMobInfos() != null) {
                for (MonsterBattleMobInfo mbmi : getMonsterBattleMobInfos()) {
                    mbmi.encode(outPacket);
                    // int int int int int int byte int int
                }
            }
            outPacket.encodeInt(getId());

            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0); // ^-- idk

            boolean hasMonsterBattleLadder = getMonsterBattleLadder() != null;
            outPacket.encodeByte(hasMonsterBattleLadder);
            if (hasMonsterBattleLadder) {
                getMonsterBattleLadder().encode(outPacket); // GW_MonsterBattleLadder_UserInfo::Decode
            }
            boolean hasMonsterBattleRankInfo = getMonsterBattleRankInfo() != null;
            outPacket.encodeByte(hasMonsterBattleRankInfo);
            if (hasMonsterBattleRankInfo) {
                getMonsterBattleRankInfo().encode(outPacket); // GW_MonsterBattleRankInfo::Decode(&dummyBLD, nSlotHyper);
            }
            outPacket.encodeByte(hasMonsterBattleRankInfo);
            // again?
            if (hasMonsterBattleRankInfo) {
                getMonsterBattleRankInfo().encode(outPacket); // GW_MonsterBattleRankInfo::Decode(&dummyBLD, nSlotHyper);
            }
        }
        if (mask.isInMask(DBChar.InventorySize)) {
            outPacket.encodeByte(getEquipInventory().getSlots());
            outPacket.encodeByte(getConsumeInventory().getSlots());
            outPacket.encodeByte(getEtcInventory().getSlots());
            outPacket.encodeByte(getInstallInventory().getSlots());
            outPacket.encodeByte(getCashInventory().getSlots());
        }

        if (mask.isInMask(DBChar.AdminShopCount)) {
            outPacket.encodeInt(0); // ???
            outPacket.encodeInt(0);
        }
        if (mask.isInMask(DBChar.ItemSlotEquip)) {
            outPacket.encodeByte(0); // ?
            List<Item> equippedItems = getEquippedInventory().getItems();
            equippedItems.sort(Comparator.comparingInt(Item::getBagIndex));
            for (Item item : equippedItems) {
                Equip equip = (Equip) item;
                if (item.getBagIndex() > 0 && item.getBagIndex() < 100) {
                    outPacket.encodeShort(equip.getBagIndex());
                    equip.encode(outPacket);
                }
            }
            outPacket.encodeShort(0);
            for (Item item : getEquippedInventory().getItems()) {
                Equip equip = (Equip) item;
                if (item.getBagIndex() > 100 && item.getBagIndex() < 1000) {
                    outPacket.encodeShort(equip.getBagIndex());
                    equip.encode(outPacket);
                }
            }
            outPacket.encodeShort(0);
            for (Item item : getEquipInventory().getItems()) {
                Equip equip = (Equip) item;
                outPacket.encodeShort(equip.getBagIndex());
                equip.encode(outPacket);
            }
            outPacket.encodeShort(0);
            // NonBPEquip::Decode
            for (Item item : getEquippedInventory().getItems()) {
                Equip equip = (Equip) item;
                if (item.getBagIndex() > 1000 && item.getBagIndex() < 1100) {
                    outPacket.encodeShort(equip.getBagIndex());
                    equip.encode(outPacket);
                }
            }
            outPacket.encodeShort(0);
            // VirtualEquipInventory::Decode
            for (Item item : getEquippedInventory().getItems()) {
                Equip equip = (Equip) item;
                if (item.getBagIndex() >= 1100 && item.getBagIndex() < 1200) {
                    outPacket.encodeShort(equip.getBagIndex());
                    equip.encode(outPacket);
                }
            }
            outPacket.encodeShort(0);
            outPacket.encodeShort(0);
            for (Item item : getEquippedInventory().getItems()) {
                Equip equip = (Equip) item;
                if (item.getBagIndex() >= 1200) {
                    outPacket.encodeShort(equip.getBagIndex());
                    equip.encode(outPacket);
                }
            }
            outPacket.encodeShort(0);
            outPacket.encodeShort(0);
            outPacket.encodeShort(0);
            outPacket.encodeShort(0);
            for (Item item : getEquippedInventory().getItems()) {
                Equip equip = (Equip) item;
                if (item.getBagIndex() >= 5000 && item.getBagIndex() <= 5003) {
                    outPacket.encodeShort(equip.getBagIndex());
                    equip.encode(outPacket);
                }
            }
            outPacket.encodeShort(0);
            outPacket.encodeShort(0);
            outPacket.encodeShort(0);
            outPacket.encodeShort(0);
            outPacket.encodeShort(0);
        }
        if (mask.isInMask(DBChar.ItemSlotConsume)) {
            for (Item item : getConsumeInventory().getItems()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
            outPacket.encodeByte(0);
        }
        if (mask.isInMask(DBChar.ItemSlotInstall)) {
            for (Item item : getInstallInventory().getItems()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
            outPacket.encodeByte(0);
        }
        if (mask.isInMask(DBChar.ItemSlotEtc)) {
            for (Item item : getEtcInventory().getItems()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
            outPacket.encodeByte(0);
        }
        if (mask.isInMask(DBChar.ItemSlotCash)) {
            for (Item item : getConsumeInventory().getItems()) {
                outPacket.encodeShort(item.getBagIndex());
                item.encode(outPacket);
            }
            outPacket.encodeByte(0);
        }
        // BagDatas
        if (mask.isInMask(DBChar.ItemSlotConsume)) {
            // TODO
            outPacket.encodeInt(0);
        }
        if (mask.isInMask(DBChar.ItemSlotInstall)) {
            // TODO
            outPacket.encodeInt(0);
        }
        if (mask.isInMask(DBChar.ItemSlotEtc)) {
            // TODO
            outPacket.encodeInt(0);
        }
        if (mask.isInMask(DBChar.ItemSlotCash)) {
            // TODO
            outPacket.encodeInt(0);
        }
        // End bagdatas
        if (mask.isInMask(DBChar.CoreAura)) {
            int val = 0;
            outPacket.encodeInt(val);
            for (int i = 0; i < val; i++) {
                outPacket.encodeInt(0);
                outPacket.encodeLong(0);
            }
        }
//        if (mask.isInMask(DBChar.Unsure)) {
//            int val = 0;
//            outPacket.encodeInt(val);
//            for (int i = 0; i < val; i++) {
//                new FileTime(0).encode(outPacket);
//                outPacket.encodeLong(0);
//            }
//        }
        if (mask.isInMask(DBChar.ItemPot)) {
            boolean hasItemPot = getItemPots() != null;
            outPacket.encodeByte(hasItemPot);
            if (hasItemPot) {
                for (int i = 0; i < getItemPots().size(); i++) {
                    getItemPots().get(i).encode(outPacket);
                    outPacket.encodeByte(i != getItemPots().size() - 1);
                }
            }
        }
        if (mask.isInMask(DBChar.SkillRecord)) {
            boolean bool = true;
            outPacket.encodeByte(bool);
            if (bool) {
                short size = 0;
                outPacket.encodeShort(size);
                for (int i = 0; i < size; i++) {
                    int skillId = 0;
                    outPacket.encodeInt(skillId);
                    outPacket.encodeInt(0); // nCount, probably current level
                    new Core(0, 0).encode(outPacket);
                    if (SkillConstants.isSkillNeedMasterLevel(skillId)) {
                        outPacket.encodeInt(0); // nCount, probably max level
                    }
                }
                short size2 = 0;
                outPacket.encodeShort(size2);
                for (int i = 0; i < size2; i++) {
                    outPacket.encodeInt(0); // another nCount
                    outPacket.encodeShort(0); // idk
                }
            } else {
                short size = 0;
                outPacket.encodeShort(size);
                for (int i = 0; i < size; i++) {
                    outPacket.encodeInt(0); // nTI
                    outPacket.encodeInt(0); // sValue
                }
                short size2 = 0;
                outPacket.encodeShort(size2);
                for (int i = 0; i < size2; i++) {
                    outPacket.encodeInt(0); // nTI
                }

                short size3 = 0;
                outPacket.encodeShort(size3);
                for (int i = 0; i < size3; i++) {
                    outPacket.encodeInt(0); // nTI
                    outPacket.encodeFT(new FileTime(0)); // pInfo
                }
                short size4 = 0;
                outPacket.encodeShort(size2);
                for (int i = 0; i < size2; i++) {
                    outPacket.encodeInt(0); // nTI
                }

                short size5 = 0;
                outPacket.encodeShort(size);
                for (int i = 0; i < size; i++) {
                    outPacket.encodeInt(0); // nTI
                    outPacket.encodeInt(0); // sValue
                }
                short size6 = 0;
                outPacket.encodeShort(size2);
                for (int i = 0; i < size2; i++) {
                    outPacket.encodeInt(0); // nTI
                }
            }
        }
        if (mask.isInMask(DBChar.SkillCooltime)) {
            short size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(0); // nSkillId
                outPacket.encodeInt(0); // nSkillCooltime
            }
        }
        if (mask.isInMask(DBChar.QuestRecord)) {
            // modified/deleted, not completed anyway
            boolean init = true;
            outPacket.encodeByte(init);
            short size = (short) getQuestManager().getQuests().size();
            outPacket.encodeShort(size);
            for (Quest quest : getQuestManager().getQuests()) {
                outPacket.encodeInt(quest.getQRKey());
                outPacket.encodeString(quest.getQRValue());
            }
            if (!init) {
                short size2 = 0;
                outPacket.encodeShort(size2);
                for (int i = 0; i < size2; i++) {
                    outPacket.encodeInt(0); // nQRKey
                }
            }
            outPacket.encodeShort(0); // ? mushy
        }
        if (mask.isInMask(DBChar.QuestComplete)) {
            boolean init = true;
            outPacket.encodeByte(init);
            List<Quest> completedQuests = getQuestManager().getCompletedQuests();
            outPacket.encodeShort(completedQuests.size());
            for (Quest quest : completedQuests) {
                outPacket.encodeInt(quest.getQRKey());
                outPacket.encodeInt(0); // Timestamp of completion
            }
            if (!init) {
                short size = 0;
                outPacket.encodeShort(size);
                for (int i = 0; i < size; i++) {
                    outPacket.encodeInt(0); // nQRKey?
                }
            }
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B FF C9 9A 3B 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if (mask.isInMask(DBChar.MinigameRecord)) {
            int size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                new MiniGameRecord().encode(outPacket);
            }
        }
        if (mask.isInMask(DBChar.CoupleRecord)) {
            int coupleSize = 0;
            outPacket.encodeShort(coupleSize);
            for (int i = 0; i < coupleSize; i++) {
                new CoupleRecord().encode(outPacket);
            }
            int friendSize = 0;
            outPacket.encodeShort(friendSize);
            for (int i = 0; i < friendSize; i++) {
                new FriendRecord().encode(outPacket);
            }
            int marriageSize = 0;
            outPacket.encodeShort(marriageSize);
            for (int i = 0; i < marriageSize; i++) {
                new MarriageRecord().encode(outPacket);
            }
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if (mask.isInMask(DBChar.MapTransfer)) {
            for (int i = 0; i < 5; i++) {
                outPacket.encodeInt(0);
            }
            for (int i = 0; i < 10; i++) {
                outPacket.encodeInt(0);
            }
            for (int i = 0; i < 13; i++) {
                outPacket.encodeInt(0);
            }
            for (int i = 0; i < 13; i++) {
                outPacket.encodeInt(0);
            }
        }
        if (mask.isInMask(DBChar.MonsterBookCover)) {
            outPacket.encodeInt(0); // nMonsterBookCoverID?
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if (mask.isInMask(DBChar.MonsterBookCard)) {
            boolean isCompleted = false;
            outPacket.encodeByte(isCompleted);
            if (!isCompleted) {
                short size = 0;
                outPacket.encodeShort(size);
                for (int i = 0; i < size; i++) {
                    outPacket.encodeShort(0);
                    outPacket.encodeByte(0);
                }
            } else {
                outPacket.encodeShort(0); // card list size
                short encSize = 0;
                outPacket.encodeShort(encSize);
                outPacket.encodeBytes(new byte[encSize]);
                encSize = 0;
                outPacket.encodeShort(encSize);
                outPacket.encodeBytes(new byte[encSize]);
            }
            outPacket.encodeInt(-1); // monsterbook set
        }
        if (mask.isInMask(DBChar.QuestCompleteOld)) {
            short size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeShort(0);
            }
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if (mask.isInMask(DBChar.Familiar)) {
            outPacket.encodeInt(0);
            // if int is > 0: sub_72AEB0
        }
        if (mask.isInMask(DBChar.NewYearCard)) {
            short size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeString("");
                outPacket.encodeByte(0);
                outPacket.encodeLong(0);
                outPacket.encodeInt(0);
                outPacket.encodeString("");
                outPacket.encodeByte(0);
                outPacket.encodeByte(0);
                outPacket.encodeLong(0);
                outPacket.encodeString("");
            }
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if (mask.isInMask(DBChar.QuestRecordEx)) {
            outPacket.encodeShort(getQuestManager().getEx().size());
            for (Quest quest : getQuestManager().getEx()) {
                outPacket.encodeInt(quest.getQRKey());
                outPacket.encodeString(quest.getQRValue());
            }
        }
        if (mask.isInMask(DBChar.Avatar)) {
            short size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(0); // sValue
                new AvatarLook().encode(outPacket);
            }
        }
        if (mask.isInMask(DBChar.MapTransfer)) {
            int size = 0;
            outPacket.encodeInt(0);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
        }
        if (mask.isInMask(DBChar.WildHunterInfo)) {
            if (JobConstants.isWildHunter(getAvatarData().getCharacterStat().getJob())) {
//                getWildHunterInfo().encode(outPacket); // GW_WildHunterInfo::Decode
            }
        }
        if (mask.isInMask(DBChar.ZeroInfo)) {
            if (JobConstants.isZero(getAvatarData().getCharacterStat().getJob())) {
//                getZeroInfo().encode(outPacket);
            }
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if (mask.isInMask(DBChar.ShopBuyLimit)) {
            short size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                // Encode shop buy limit
            }
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if (mask.isInMask(DBChar.StolenSkills)) {
            if (JobConstants.isPhantom(getAvatarData().getCharacterStat().getJob())) {
                for (Skill skill : getStolenSkills()) {
                    outPacket.encodeInt(skill.getSkillId());
                }
            } else {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);

                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);

                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);

                outPacket.encodeInt(0);
                outPacket.encodeInt(0);

                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
            }
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if (mask.isInMask(DBChar.ChosenSkills)) {
            if (JobConstants.isPhantom(getAvatarData().getCharacterStat().getJob())) {
                for (Skill skill : getChosenSkills()) {
                    outPacket.encodeInt(skill.getSkillId());
                }
            } else {
                for (int i = 0; i < 5; i++) {
                    outPacket.encodeInt(0);
                }
            }
        }
        if (mask.isInMask(DBChar.CharacterPotentialSkill)) {
            short size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeByte(0); // nKey
                outPacket.encodeInt(0); // nSkillID
                outPacket.encodeByte(0); // nSLV
                outPacket.encodeByte(0); // nGrade
            }
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if(mask.isInMask(DBChar.SoulCollection)) {
            short size = 0;
            outPacket.encodeShort(size);
            for(int i = 0; i < size; i++) {
                outPacket.encodeInt(0); //
                outPacket.encodeInt(0); //
            }
        }
        sizee = 0;
        outPacket.encodeInt(sizee);
        for (int i = 0; i < sizee; i++) {
            outPacket.encodeString("");
            // sub_73A1A0
            outPacket.encodeInt(0);
            outPacket.encodeString("");
            int size = 0;
            outPacket.encodeInt(size);
            for (int j = 0; j < size; j++) {
                outPacket.encodeByte(0);
            }
        }
        outPacket.encodeByte(0); // idk
        // MonsterLifeInviteInfo
//        int sizeInt = 0;
//        for(int i = 0; i < sizeInt; i++) {
//            getMonsterLifeInviteInfo().encode(outPacket);
//        }
        //outPacket.encodeBytes(Util.getByteArrayByString("01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if (mask.isInMask(DBChar.Character)) {
            outPacket.encodeInt(0); // honor level
            outPacket.encodeInt(0); // honor exp
        }
        if (mask.isInMask(DBChar.Money)) {
            boolean shouldIEncodeThis = true;
            outPacket.encodeByte(shouldIEncodeThis);
            if (shouldIEncodeThis) {
                short size = 0;
                outPacket.encodeShort(size);
                for (int i = 0; i < size; i++) {
                    short category = 0;
                    outPacket.encodeShort(category);
                    short size2 = 0;
                    outPacket.encodeShort(size2);
                    for (int i2 = 0; i2 < size2; i2++) {
                        outPacket.encodeInt(0); // nItemId
                        outPacket.encodeInt(0); // nCount
                    }
                }
            } else {
                short size2 = 0;
                outPacket.encodeShort(size2);
                for (int i2 = 0; i2 < size2; i2++) {
                    outPacket.encodeShort(0); // nCategory
                    outPacket.encodeInt(0); // nItemId
                    outPacket.encodeInt(0); // nCount
                }

            }
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if (mask.isInMask(DBChar.ReturnEffectInfo)) {
//            getReturnEffectInfo().encode(outPacket); // ReturnEffectInfo::Decode
            outPacket.encodeByte(0);
        }
        if (mask.isInMask(DBChar.DressUpInfo)) {
            new DressUpInfo().encode(outPacket); // GW_DressUpInfo::Decode
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if (mask.isInMask(DBChar.Unk2)) {
            outPacket.encodeInt(1);
            outPacket.encodeInt(0);
            outPacket.encodeLong(0);
            outPacket.encodeString("");
            outPacket.encodeInt(0);
        }
        if (mask.isInMask(DBChar.CoreInfo)) {
            // GW_Core
            short size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeShort(0); // nPos
                outPacket.encodeInt(0); // nCoreID
                outPacket.encodeInt(0); // nLeftCount
            }

            size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeShort(0); // nPos
                outPacket.encodeInt(0); // nCoreID
                outPacket.encodeInt(0); // nLeftCount
            }
        }
        if (mask.isInMask(DBChar.FarmPotential)) {
            new FarmPotential().encode(outPacket); // FARM_POTENTIAL::Decode
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if (mask.isInMask(DBChar.FarmUserInfo)) {
            new FarmUserInfo().encode(outPacket); // FarmUserInfo::Decode
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        if (mask.isInMask(DBChar.MemorialCubeInfo)) {
            new MemorialCubeInfo().encode(outPacket); // MemorialCubeInfo::Decode
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        if (mask.isInMask(DBChar.LikePoint)) {
            new LikePoint().encode(outPacket);
        }
        if (mask.isInMask(DBChar.RunnerGameRecord)) {
            new RunnerGameRecord().encode(outPacket);
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("00 00 00 00 00 00 00 00 00 01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*
        short sizeO = 0;
        outPacket.encodeShort(sizeO);
        for (int i = 0; i < sizeO; i++) {
            outPacket.encodeInt(0);
            outPacket.encodeString("");

        }
        if (mask.isInMask(DBChar.Unk2)) { // Mushy: Monster Collection
            int size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(0); // same as above?
                outPacket.encodeString("");
            }
        }
        boolean farmOnline = false;
        outPacket.encodeByte(farmOnline);
        int sizeInt = 0;
        // CharacterData::DecodeTextEquipInfo
        outPacket.encodeInt(sizeInt);
        for (int i = 0; i < sizeInt; i++) {
            outPacket.encodeInt(0);
            outPacket.encodeString("");
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("01 00 01 00 00 00 00 00 00 00 64 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*

        if (mask.isInMask(DBChar.VisitorLog4)) {
            // mushy
            outPacket.encodeByte(1);
            outPacket.encodeByte(0);
            outPacket.encodeInt(1);
            outPacket.encodeInt(0);
            outPacket.encodeInt(100);
            outPacket.encodeLong(FileTime.getFileTimeFromType(FileTime.Type.MAX_TIME));
            outPacket.encodeShort(0);
            outPacket.encodeShort(0);
        }

        if (mask.isInMask(DBChar.Unk4)) {
            outPacket.encodeByte(0);
        }

        if (mask.isInMask(DBChar.Unk)) {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 01 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*

        if (mask.isInMask(DBChar.CoreAura)) {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);

            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);

            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);

            outPacket.encodeLong(0);
            outPacket.encodeByte(0);

            outPacket.encodeByte(1);
        }
        //outPacket.encodeBytes(Util.getByteArrayByString("00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 64 2B 70 84 7A D3 01 64 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));/*

        if (mask.isInMask(DBChar.EquipExt)) {
            short size = 0;
            outPacket.encodeShort(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeShort(0);
                outPacket.encodeShort(0);
            }
        }

        if (mask.isInMask(DBChar.RedLeafInfo)) {
            // red leaf information
            outPacket.encodeInt(getAccId());
            outPacket.encodeInt(getId());
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        outPacket.encodeBytes(new byte[32]); // real
    }

    private String getBlessingOfEmpress() {
        return null;
    }

    private String getBlessingOfFairy() {
        return null;
    }

    public void setCombatOrders(int combatOrders) {
        this.combatOrders = combatOrders;
    }

    public int getCombatOrders() {
        return combatOrders;
    }

    public List<Skill> getStolenSkills() {
        return stolenSkills;
    }

    public List<Skill> getChosenSkills() {
        return chosenSkills;
    }

    public void setChosenSkills(List<Skill> chosenSkills) {
        this.chosenSkills = chosenSkills;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public void setQuests(QuestManager questManager) {
        this.questManager = questManager;
    }

    public List<ItemPot> getItemPots() {
        return null;
    }

    public void setItemPots(List<ItemPot> itemPots) {
        this.itemPots = itemPots;
    }

    public List<Pet> getPets() {
        return pets;
    }

    public void setPets(List<Pet> pets) {
        this.pets = pets;
    }

    public List<FriendRecord> getFriends() {
        return friends;
    }

    public void setFriends(List<FriendRecord> friends) {
        this.friends = friends;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public List<ExpConsumeItem> getExpConsumeItems() {
        return expConsumeItems;
    }

    public void setExpConsumeItems(List<ExpConsumeItem> expConsumeItems) {
        this.expConsumeItems = expConsumeItems;
    }

    public List<MonsterBattleMobInfo> getMonsterBattleMobInfos() {
        return monsterBattleMobInfos;
    }

    public void setMonsterBattleMobInfos(List<MonsterBattleMobInfo> monsterBattleMobInfos) {
        this.monsterBattleMobInfos = monsterBattleMobInfos;
    }

    public MonsterBattleLadder getMonsterBattleLadder() {
        return monsterBattleLadder;
    }

    public void setMonsterBattleLadder(MonsterBattleLadder monsterBattleLadder) {
        this.monsterBattleLadder = monsterBattleLadder;
    }

    public MonsterBattleRankInfo getMonsterBattleRankInfo() {
        return monsterBattleRankInfo;
    }

    public void setMonsterBattleRankInfo(MonsterBattleRankInfo monsterBattleRankInfo) {
        this.monsterBattleRankInfo = monsterBattleRankInfo;
    }

    public List<Inventory> getInventories() {
        return new ArrayList<>(Arrays.asList(getEquippedInventory(), getEquipInventory(),
                getConsumeInventory(), getEtcInventory(), getInstallInventory(), getCashInventory()));
    }

    public Inventory getInventoryByInvType(InvType invType) {
        switch(invType) {
            case EQUIPPED:
                return getEquippedInventory();
            case EQUIP:
                return getEquipInventory();
            case USE:
                return getConsumeInventory();
            case ETC:
                return getEtcInventory();
            case SETUP:
                return getInstallInventory();
            case CASH:
                return getCashInventory();
            default:
                return null;
        }
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public int getFieldID() {
        return fieldID;
    }

    public void setFieldID(int fieldID) {
        this.fieldID = fieldID;
    }

    public boolean hasBuffProtector() {
        // TODO
        return false;
    }
}
