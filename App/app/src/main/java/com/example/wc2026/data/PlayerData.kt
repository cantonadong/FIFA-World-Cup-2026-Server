package com.carldong.fifa.worldcup2026.data

object PlayerData {
    val all: List<Player> = listOf(
        Player(1,"K. Mbappé","Kylian Mbappé","France","France.png","fr","Real Madrid","es","FW",27,95,97,93,84,96,37,78,180,"178 cm","73 kg","R",77,48,22,"pic/player/player_1.png"),
        Player(2,"L. Messi","Lionel Messi","Argentina","Argentina.png","ar","Inter Miami","us","FW",38,94,65,91,91,95,36,66,35,"170 cm","72 kg","L",191,109,56,"pic/player/player_2.png"),
        Player(3,"Vinícius Jr.","Vinícius Júnior","Brazil","Brazil.png","br","Real Madrid","es","FW",24,92,96,86,80,94,30,73,180,"176 cm","73 kg","R",40,12,7,"pic/player/player_3.png"),
        Player(4,"J. Bellingham","Jude Bellingham","England","England.png","gb-eng","Real Madrid","es","MF",21,91,78,82,87,88,75,82,180,"186 cm","75 kg","R",38,12,8,"pic/player/player_4.png"),
        Player(5,"H. Kane","Harry Kane","England","England.png","gb-eng","Bayern Munich","de","FW",31,90,71,93,83,83,47,83,70,"188 cm","86 kg","R",96,68,18,"pic/player/player_5.png"),
        Player(6,"A. Becker","Alisson Becker","Brazil","Brazil.png","br","Liverpool","gb-eng","GK",32,90,55,16,80,60,14,68,50,"191 cm","91 kg","R",78,0,5,"pic/player/player_6.png"),
        Player(7,"M. ter Stegen","Marc-André ter Stegen","Germany","Germany.png","de","Barcelona","es","GK",33,90,55,15,82,62,13,72,45,"187 cm","85 kg","R",43,0,3,"pic/player/player_7.png"),
        Player(8,"P. Foden","Phil Foden","England","England.png","gb-eng","Man City","gb-eng","MF",24,89,84,85,88,91,55,67,150,"171 cm","69 kg","L",38,11,9,"pic/player/player_8.png"),
        Player(9,"P. Pedri","Pedro González","Spain","Spain.png","es","Barcelona","es","MF",22,89,77,78,91,90,65,68,100,"174 cm","60 kg","R",42,7,10,"pic/player/player_9.png"),
        Player(10,"J. Kimmich","Joshua Kimmich","Germany","Germany.png","de","Bayern Munich","de","MF",29,89,72,76,92,84,86,79,60,"177 cm","73 kg","R",83,18,27,"pic/player/player_10.png"),
        Player(11,"J. Musiala","Jamal Musiala","Germany","Germany.png","de","Bayern Munich","de","MF",21,88,83,84,85,91,53,68,150,"180 cm","70 kg","R",36,13,9,"pic/player/player_11.png"),
        Player(12,"B. Saka","Bukayo Saka","England","England.png","gb-eng","Arsenal","gb-eng","FW",23,88,85,84,85,89,62,68,150,"178 cm","72 kg","L",52,16,15,"pic/player/player_12.png"),
        Player(13,"L. Martínez","Lautaro Martínez","Argentina","Argentina.png","ar","Inter Milan","it","FW",27,87,83,88,77,84,42,78,65,"174 cm","72 kg","R",58,30,14,"pic/player/player_13.png"),
        Player(14,"V. Osimhen","Victor Osimhen","Nigeria","Nigeria.png","ng","Galatasaray","tr","FW",26,87,91,87,70,84,39,85,75,"185 cm","78 kg","R",47,22,9,"pic/player/player_14.png"),
        Player(15,"K. Havertz","Kai Havertz","Germany","Germany.png","de","Arsenal","gb-eng","FW",25,85,80,83,79,82,52,78,70,"194 cm","83 kg","L",52,26,10,"pic/player/player_15.png"),
        Player(16,"R. De Paul","Rodrigo De Paul","Argentina","Argentina.png","ar","Atlético Madrid","es","MF",30,85,80,78,86,84,74,80,30,"180 cm","75 kg","R",87,29,33,"pic/player/player_16.png"),
        Player(17,"S. Amrabat","Sofyan Amrabat","Morocco","Morocco.png","ma","Fiorentina","it","MF",28,83,73,63,80,75,84,83,35,"184 cm","79 kg","R",56,3,6,"pic/player/player_17.png"),
        Player(18,"W. Endo","Wataru Endo","Japan","Japan.png","jp","Liverpool","gb-eng","MF",31,83,68,62,79,74,85,80,25,"175 cm","75 kg","R",73,8,12,"pic/player/player_18.png"),
        Player(19,"H. Ziyech","Hakim Ziyech","Morocco","Morocco.png","ma","Galatasaray","tr","FW",32,82,78,79,83,82,32,63,12,"181 cm","70 kg","L",67,23,16,""),
        Player(20,"S. Mitoma","Kaoru Mitoma","Japan","Japan.png","jp","Brighton","gb-eng","FW",27,82,89,79,78,86,40,62,42,"178 cm","71 kg","L",28,9,7,"pic/player/player_20.png"),
    )

    val byFullName: Map<String, Player> = all.associateBy { it.full }

    /** Detailed EA FC25 position for each player (id → position code) */
    val detailedPos: Map<Int, String> = mapOf(
        1  to "LW",   // Mbappé
        2  to "CF",   // Messi
        3  to "LW",   // Vinícius Jr.
        4  to "CAM",  // Bellingham
        5  to "ST",   // Kane
        6  to "GK",   // Alisson Becker
        7  to "GK",   // ter Stegen
        8  to "CAM",  // Foden
        9  to "CM",   // Pedri
        10 to "CDM",  // Kimmich
        11 to "CAM",  // Musiala
        12 to "RW",   // Saka
        13 to "ST",   // Lautaro Martínez
        14 to "ST",   // Osimhen
        15 to "LW",   // Havertz
        16 to "CM",   // De Paul
        17 to "CDM",  // Amrabat
        18 to "CDM",  // Endo
        19 to "CAM",  // Ziyech
        20 to "LW",   // Mitoma
    )
}

