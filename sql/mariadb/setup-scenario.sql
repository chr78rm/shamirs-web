--
-- cleanup
--
DELETE FROM slice;
DELETE FROM keystore;
DELETE FROM participant;

--
-- setup keystore
--
INSERT INTO keystore (id, descriptive_name, store, creation_time, modification_time) 
VALUES (
    '5adab38c-702c-4559-8a5f-b792c14b9a43',
    'my-first-keystore',
    0x308205500201033082050906092a864886f70d010701a08204fa048204f6308204f2308201ee06092a864886f70d010701a08201df048201db308201d73081dd060b2a864886f70d010c0a0105a07a3078060b2a864886f70d010c0a0102a069046730653029060a2a864886f70d010c0103301b04142987b1cff511a7b6788cbe9fe344c9da416fc5ea020300c35004385fd8b66fe45705a393aa6007ccb6fe1452c94e829d050cf9cb248408ab032851dbf2915b54d7e18596ce688b5a4f232395b091e6183d29f73152302d06092a864886f70d01091431201e1e0074006500730074002d007300650063007200650074002d006b00650079302106092a864886f70d0109153114041254696d6520313630303236303935363633333081f4060b2a864886f70d010c0a0102a081983081953029060a2a864886f70d010c0103301b0414fbad6bcf1159531d006324c8ab517298e3194005020300c3500468e7ede4dfe0676805108c9c3e884eaedd27fa3eeab278a35419819db050acdc786ec7f13cc0dfe9f560bd873c02cf9d62212881f2573afd4566fe4492b2656c7f3a6e3ff42496e3107f93e8c5b002e0dbe508395526cdffd084f9db063a6ec51e652e157ebf05dc82314a302506092a864886f70d01091431181e160074006500730074002d00650063002d006b00650079302106092a864886f70d0109153114041254696d652031363030323631303439333737308202fc06092a864886f70d010706a08202ed308202e9020100308202e206092a864886f70d0107013029060a2a864886f70d010c0106301b0414d36b94da44fe892f89ccfe5d08d35dc539bf1797020300c350808202a83a196422fd89ac97a638b58fda86662d9122d6d346c00f340a16b55670ad3865ed7c391e38e5e59e990a3c71fbed1dd81b2c6bf843cc715acdca4c824d0a8bbc59bd6ed332cff0f506c6b1a59f1a870eb4b41af92b050f8928bf062c9ed3154288e997432bd8ab25f8a822b7c55e443e19526f225e7466a62efa34008c09371034bed74b40490cbb1c99e5f20225f9effc056b7bc228bbbeeba5ba41b7d0834955b97ae5b280da6e724ec3f8301573b159eee1ce4c00e5bef336fbf8b0950d680556ffcd31c0c79365a0e0def1f31e0f8b8a9646467f46e505418bd9e4ab5c5395264ddf302deed9249239c56285f1a3d15eaef594124cfd10e542034a8808aabb9e90f285033bc2dd68b8ca836cc1758262081fab1558c241535abf7653c92b557adb5675fb3d6eefeb770b4a72e22c6da973f50485ffa015f035e6b20996c95cd39140fe48ede0a204747f63e99aa6c6ceb66062e22a75dcac03f8500a5bc061653ff94251b334f0e195ca3f03def4065443c4ce0e57cb540aee7d97167660225d62baaea920c13eb64ae710f2e2f28dc3d4d7aa6b04782a00a2d8177b00b5997769a6265760fd2d98bc7099e0639d44bf3b95a3776bdb4f51e165974697abe56f32fe6a0c60120baa3d6724ffc4e3b930c02fd6a1e59634d249ed2b310858d6636bcbc687bbe686bbac1c12e31f54e6c156474b2390c826bb5a49ed23e2bf7674d02b30eb95f54b63f1d670f06babd50e591717c1d23a87705570e7bb29e722765a1adbb80318bc2c8543b581377a279808764a336b6a8862706f5cb148cdadc807dbb40d03fa2e58ed21459d3505d1539df055f155a8df2f7562fc321d528f7a202c6ce7d78b84bbd424f500ae190ba5ecff495a6f05bb116f695e95416bdcd025aadc067e918115961df5a18e875f45c27ff486be3207bebe71f4980f32822409d8f2456aea303e3021300906052b0e03021a0500041476fdcdeba43d75edf83d61f0d27295c382603531041473fd06383efb43afd729bc26dde99381289f923a02030186a0, 
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

--
-- setup participant
--
INSERT INTO participant (id, preferred_name, effective_time)
VALUES (
    '8844dd34-c836-4060-ba73-c6d86ad1275d',
    'christof',
    CURRENT_TIMESTAMP
);
INSERT INTO participant (id, preferred_name, effective_time)
VALUES (
    'f6cdb2e5-ea3e-405f-ad0a-14c034497e23',
    'test-user-1',
    CURRENT_TIMESTAMP
);
INSERT INTO participant (id, preferred_name, effective_time)
VALUES (
    '337dd2bd-508d-423d-84ca-81770d8ac30d',
    'test-user-2',
    CURRENT_TIMESTAMP
);
INSERT INTO participant (id, preferred_name, effective_time)
VALUES (
    '48ef6c98-0e04-49bc-9f7f-01f2cec3ccac',
    'test-user-3',
    CURRENT_TIMESTAMP
);
INSERT INTO participant (id, preferred_name, effective_time)
VALUES (
    '222185fb-6cbc-45e6-90d1-e5390fb2f9f9',
    'test-user-4',
    CURRENT_TIMESTAMP
);
INSERT INTO participant (id, preferred_name, effective_time)
VALUES (
    'b78d63a0-e365-4934-93e4-ec1ea713cba8',
    'test-user-5',
    CURRENT_TIMESTAMP
);
INSERT INTO participant (id, preferred_name, effective_time)
VALUES (
    '54ce43ce-c335-47a2-98b8-1bd1fc4f93a4',
    'test-user-6',
    CURRENT_TIMESTAMP
);

--
-- setup slice
--
INSERT INTO slice (id, participant_id, keystore_id, partition_id, share, processing_state, effective_time)
VALUES (
    '9a83d398-35d6-4959-aea2-1c930a936b43',
    '8844dd34-c836-4060-ba73-c6d86ad1275d', -- christof
    '5adab38c-702c-4559-8a5f-b792c14b9a43', -- my-first-keystore
    '467b268d-1a7f-4f00-993c-672b82494822',
    0x0a7b0a20202020224964223a202234363762323638642d316137662d346630302d393933632d363732623832343934383232222c0a20202020225072696d65223a203331353933343032323438303539353738383639303834363735363234333730353836333434393534383034373038333233383136373432333535323633312c0a20202020225468726573686f6c64223a20342c0a20202020225368617265506f696e7473223a205b0a20202020202020207b0a202020202020202020202020225368617265506f696e74223a207b0a202020202020202020202020202020202278223a203238393534333232373135383239383833383533363036313136333030313433383531363434393433363732353439323735343333343135303938383832342c0a202020202020202020202020202020202279223a203234323937393930363533303234363735303430393338343730313531343031383837333030373334383032373635333632313335373533343938363037320a2020202020202020202020207d0a20202020202020207d2c0a20202020202020207b0a202020202020202020202020225368617265506f696e74223a207b0a202020202020202020202020202020202278223a203231323637353234373433303034323338343332343134323332343337303239383139373530323534373330363733393034353239303331363737373835382c0a202020202020202020202020202020202279223a203132303732313439323539313535383730393635333932343437313833353334333134363731353130323236373230323030353839323634323533393031320a2020202020202020202020207d0a20202020202020207d2c0a20202020202020207b0a202020202020202020202020225368617265506f696e74223a207b0a202020202020202020202020202020202278223a203231383932383835393836363833363436343430383230303435373631323635373738373635353332303832383939383237343636373730363435373436392c0a202020202020202020202020202020202279223a203136353937353637323137333337393431323031343130363038353431373731333933373136303835323537363433353339343936313330303034343431310a2020202020202020202020207d0a20202020202020207d2c0a20202020202020207b0a202020202020202020202020225368617265506f696e74223a207b0a202020202020202020202020202020202278223a2037363937333535383932323537323237323337353534323134353230343135373336363137363432363836393939323237333934373838333534353736302c0a202020202020202020202020202020202279223a2033383234373834313331333332363234313738323833383634363736383837393638363236323433353135373739333133373134303332373539383430380a2020202020202020202020207d0a20202020202020207d0a202020205d0a7d,
    'POSTED',
    CURRENT_TIMESTAMP
);
INSERT INTO slice (id, participant_id, keystore_id, partition_id, share, processing_state, effective_time)
VALUES (
    '4f66bb7d-417d-48d8-a269-e0d2011715f1',
    'f6cdb2e5-ea3e-405f-ad0a-14c034497e23', -- test-user-1
    '5adab38c-702c-4559-8a5f-b792c14b9a43', -- my-first-keystore
    '467b268d-1a7f-4f00-993c-672b82494822',
    0x0a7b0a20202020224964223a202234363762323638642d316137662d346630302d393933632d363732623832343934383232222c0a20202020225072696d65223a203331353933343032323438303539353738383639303834363735363234333730353836333434393534383034373038333233383136373432333535323633312c0a20202020225468726573686f6c64223a20342c0a20202020225368617265506f696e7473223a205b0a20202020202020207b0a202020202020202020202020225368617265506f696e74223a207b0a202020202020202020202020202020202278223a203134343339383139363630303538303431363730393837353230343032353839323939363234383030303535303935373431303737353235303137313035362c0a202020202020202020202020202020202279223a203130343431353230353639343234383739373532323230333138383436363137383033353939323839323938313437393433323338303431393835383737360a2020202020202020202020207d0a20202020202020207d2c0a20202020202020207b0a202020202020202020202020225368617265506f696e74223a207b0a202020202020202020202020202020202278223a203331343730393531333937323736333730313630393330313837363135373634363831383032343636323231383031323837303130373432383434353633332c0a202020202020202020202020202020202279223a203139303732363539363430343435373833303739333333363336313333303639303038303332343332363930373032323035313630393536303234333433350a2020202020202020202020207d0a20202020202020207d0a202020205d0a7d,
    'POSTED',
    CURRENT_TIMESTAMP
);
INSERT INTO slice (id, participant_id, keystore_id, partition_id, share, processing_state, effective_time)
VALUES (
    '35650def-5d15-40d8-a707-21ecf9799d1d',
    '337dd2bd-508d-423d-84ca-81770d8ac30d', -- test-user-2
    '5adab38c-702c-4559-8a5f-b792c14b9a43', -- my-first-keystore
    '467b268d-1a7f-4f00-993c-672b82494822',
    0x0a7b0a20202020224964223a202234363762323638642d316137662d346630302d393933632d363732623832343934383232222c0a20202020225072696d65223a203331353933343032323438303539353738383639303834363735363234333730353836333434393534383034373038333233383136373432333535323633312c0a20202020225468726573686f6c64223a20342c0a20202020225368617265506f696e7473223a205b0a20202020202020207b0a202020202020202020202020225368617265506f696e74223a207b0a202020202020202020202020202020202278223a203136313738333837313935323734343735343039333136313837373632343233343532303438313631323238343434343831373530303133353534313532362c0a202020202020202020202020202020202279223a2031343137363731383535323034333033393238373835313237303433353935383632353734333934323734303739383730393439393338383730353237390a2020202020202020202020207d0a20202020202020207d2c0a20202020202020207b0a202020202020202020202020225368617265506f696e74223a207b0a202020202020202020202020202020202278223a203331333330393234363634363937313338373036363232343038353131323932353636373331353834373632303937313833373331343331313537373239382c0a202020202020202020202020202020202279223a203232313231383336363039343835353138333931303533333834383036353638333034343932353634373233313331353231333337313830303136313631310a2020202020202020202020207d0a20202020202020207d0a202020205d0a7d,
    'POSTED',
    CURRENT_TIMESTAMP
);
INSERT INTO slice (id, participant_id, keystore_id, partition_id, share, processing_state, effective_time)
VALUES (
    '187b30af-65f6-4bb1-8feb-68263dcdffa7',
    '48ef6c98-0e04-49bc-9f7f-01f2cec3ccac', -- test-user-3
    '5adab38c-702c-4559-8a5f-b792c14b9a43', -- my-first-keystore
    '467b268d-1a7f-4f00-993c-672b82494822',
    0x0a7b0a20202020224964223a202234363762323638642d316137662d346630302d393933632d363732623832343934383232222c0a20202020225072696d65223a203331353933343032323438303539353738383639303834363735363234333730353836333434393534383034373038333233383136373432333535323633312c0a20202020225468726573686f6c64223a20342c0a20202020225368617265506f696e7473223a205b0a20202020202020207b0a202020202020202020202020225368617265506f696e74223a207b0a202020202020202020202020202020202278223a203232353432353836333139323434303530353839313933353838383839373537313532313938383932323235313135393232343830323031363133383832372c0a202020202020202020202020202020202279223a2034393237393530383131363935363535303033333936323933313437303935393533353633313935353139393237393135333234323233323337323730380a2020202020202020202020207d0a20202020202020207d0a202020205d0a7d,
    'POSTED',
    CURRENT_TIMESTAMP
);
INSERT INTO slice (id, participant_id, keystore_id, partition_id, share, processing_state, effective_time)
VALUES (
    '6dc636e7-efa8-4e30-9ee3-a373e8063e30',
    '222185fb-6cbc-45e6-90d1-e5390fb2f9f9', -- test-user-4
    '5adab38c-702c-4559-8a5f-b792c14b9a43', -- my-first-keystore
    '467b268d-1a7f-4f00-993c-672b82494822',
    0x0a7b0a20202020224964223a202234363762323638642d316137662d346630302d393933632d363732623832343934383232222c0a20202020225072696d65223a203331353933343032323438303539353738383639303834363735363234333730353836333434393534383034373038333233383136373432333535323633312c0a20202020225468726573686f6c64223a20342c0a20202020225368617265506f696e7473223a205b0a20202020202020207b0a202020202020202020202020225368617265506f696e74223a207b0a202020202020202020202020202020202278223a2037383438373639353831383538363336363030343639373534353137383637363831313034383839363734373034393731393435363932353831393235322c0a202020202020202020202020202020202279223a203236313032383130343139323132333537323136323039373438313832373739323934393037303831303737343438373631313037343637373536383036320a2020202020202020202020207d0a20202020202020207d0a202020205d0a7d,
    'POSTED',
    CURRENT_TIMESTAMP
);
INSERT INTO slice (id, participant_id, keystore_id, partition_id, share, processing_state, effective_time)
VALUES (
    '6ef561a2-020a-492e-abb3-106a467a4908',
    'b78d63a0-e365-4934-93e4-ec1ea713cba8', -- test-user-5
    '5adab38c-702c-4559-8a5f-b792c14b9a43', -- my-first-keystore
    '467b268d-1a7f-4f00-993c-672b82494822',
    0x0a7b0a20202020224964223a202234363762323638642d316137662d346630302d393933632d363732623832343934383232222c0a20202020225072696d65223a203331353933343032323438303539353738383639303834363735363234333730353836333434393534383034373038333233383136373432333535323633312c0a20202020225468726573686f6c64223a20342c0a20202020225368617265506f696e7473223a205b0a20202020202020207b0a202020202020202020202020225368617265506f696e74223a207b0a202020202020202020202020202020202278223a203231373537383238373535353434323139363838383338383137393334373337343332313239313133343839373239353331353031383835343537343535302c0a202020202020202020202020202020202279223a2036343533373131373633383332323938363734363131333334333139363736393037393734373535383535363931373836303131363735393732363531300a2020202020202020202020207d0a20202020202020207d0a202020205d0a7d,
    'POSTED',
    CURRENT_TIMESTAMP
);
INSERT INTO slice (id, participant_id, keystore_id, partition_id, share, processing_state, effective_time)
VALUES (
    '7b5ab104-a05c-4103-9582-303be0dcb173',
    '54ce43ce-c335-47a2-98b8-1bd1fc4f93a4', -- test-user-6
    '5adab38c-702c-4559-8a5f-b792c14b9a43', -- my-first-keystore
    '467b268d-1a7f-4f00-993c-672b82494822',
    0x0a7b0a20202020224964223a202234363762323638642d316137662d346630302d393933632d363732623832343934383232222c0a20202020225072696d65223a203331353933343032323438303539353738383639303834363735363234333730353836333434393534383034373038333233383136373432333535323633312c0a20202020225468726573686f6c64223a20342c0a20202020225368617265506f696e7473223a205b0a20202020202020207b0a202020202020202020202020225368617265506f696e74223a207b0a202020202020202020202020202020202278223a203136323431373631303335333330303633363337343839393531323531373136393430323630353532353738353830333130313033323436393138373238302c0a202020202020202020202020202020202279223a203138373431373934313632363734313639383334343032323735303830383136303531363236363731383530323233353933343336363035323738343532300a2020202020202020202020207d0a20202020202020207d0a202020205d0a7d,
    'POSTED',
    CURRENT_TIMESTAMP
);
