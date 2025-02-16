package io.newm.chain.cardano

import io.newm.chain.util.extractStakeAddress
import org.junit.jupiter.api.Test

class ExtractStakeAddressTest {
    @Test
    fun `test extract stake address`() {
        val addresses = listOf(
            "addr1qyeg2l9h5jfa5nw7zawta9ym3raxchtte6y2h46zavth97dll8vzmue2p4zsnp77mgepqqpd68v5ujfagwj2z04mp6jsjkee3y",
            "addr1q95a9mf749qmlpwsftzxzvxdr6zts6tfrt5gyr3zf7uswlq87knz0d4xk9axkaxpcyqdw526mlxp65der6dc9829647qftnyu4",
            "addr1q9g8hpckj8pmhn45v30wkrqfnnkfftamja3y9tcyjrg44cl0wk8n4atdnas8krf94kulzdqsltujm5gzas8rgel2uw0sjk4gt8",
            "addr1q9m7gwkqtvhghk6sq6wmf6fffucfs0psreedyvu69eu5qz9hzcsh3r5qjgwu3a5j8c33wdwqrd4dffv8spr8yllg5f3s706lak",
            "addr1q8ef54u2f95ec0wgc7u9cm7g5svmfhyq3qh36y9cfdp7hr5mcsca80n59vprskll4s283nf2lje43a6qcnxtzfamg7pqmlwg68",
            "addr1q888gq49p3nrj8njpqfmncfqr88j54pt86cztpnatyfle4h43u2cep8z0qlv28ndcj55hqplaw8uap0lvzq5gssluzyswhhcn2",
            "addr1q9pkpse36ljcckuxjze360d93aymda73au3jved627l0ppfx5au8sl7fqg0maeqgjn4q5z5stt564u5s0tj9ke2gqq2s67l9h4",
            "addr1qyptf3762ygukn9ceun5qdmavl3spjnduxzur8vgpjszmegsfx4nxjufsf4v7xegkjtrjuw367xk5eggcdxjkr4dawmq65p073",
            "addr1qxf7q0cc0um7q4nqdtxv8pyckm54ah9deyjydth7vzjdchycjxxte3nv45jdqmu9l7gsewvnfr4r9r70m64mklu09hmsnl00dq",
            "addr1q920wrvpwf8j4grfmn0md9zcce0h0vwxj5r6srsm2cs05nq68f5dntvntauaunmaqs3kzhq26lul3zqg09qhhmt7y96s5us5qh",
            "addr1qxjumm7n97l58zh5fnkc5gzeyms50j8zhs0j7rv66627526v2lplery87uhaf0k05qrrshfu7x7q6dr65y0uqcev4gms5ej833",
            "addr1qxeuu9ln6k5etype7284al0rk0n8hq2c7kkj2yzpw4pxcyqrjtv2af755982e2rqcztv9pfs6zsmfrjypuv9znvvnjmqcqwc5y",
            "addr1q8feeraznwme7dndzc9n3h2fdwr2jp2st2tyv5x0pkchrs97a7jsseen3hlemdnmrav7fh66hru63207znsdqg2crjpq0ml6fe",
            "addr1qxla9s4vdjngrl9h7s8xuke80envxfa4sz5sy9ewma0us4ssvfm4xxj4x3vnfskk8c294vkwvhn26757rqp87st8vprqmwwc9g",
            "addr1q87amlzrkc70d8278nksxlrrn2pte60ztllrnw5mege3ymfjj4d0hh3w25jkyycqm8u5qyfzedsvg936fxf8hzqplw7qg3gfxv",
            "addr1q9vp7enyxga4275sjar29wxfj469f04aa4m5wy3f9zf4tsfd9hcnz7lhdg78dpsas4qck6x9slslmndvdsn8tt7ktyeq3a5lxy",
            "addr1q8j95tn98gnr8pvpykzeaqd5pkmt7c55cs07slhsd7m3etvvk9ney8pgspgzm28q587uj9d9gs2jgms9ds252dhg3ynqte95z8",
            "addr1qxk7wvuhdd67lar3tykyfvdemrsnqund002vkdg6ma4e0ynxxvzmhpy5vmewvt3nfhe9pzza8avwdn3xjru5fzfqcmcsdn5vfu",
            "addr1q8sx5pqaaa3vvnzn7hpfh98563fkw7z6hlzxrpc7cyt0jyhzpujfjh5na89dhj85yjm4p0cjyws38kzxw2pxc0290casprd8sh",
            "addr1qxlwnvktyfskgqxn9ume9lhj4ljgmhjjlrjaxnzxyylvwglvcwftdqw4sl6ugtcf33z8g39jz3ncm3wjd6uv6pyl3rwqppqck3",
            "addr1q9tz7s9juzukvqc63ctufr83xv4x370d96qx05xn3mtllmvcrvxjq6pdcvfnk4cm3dqfuckwy0slcxr8awlx7hv8hfnsutjph9",
            "addr1q86zktyyu8pl5gxfu54cc9evkgh9yx6yjjdl6c2hxx0j7d8k230axzz4pc7p75t9rtuydajcexuk9w2ezyfayfzuukpqfgtmvw",
            "addr1qycytw6f6cs3y85esyghmq7ymnp2l444794a2aewrhvp6hrg8ywec8v2vfuyla8n3rw2nwa4alsv92f5fn6mgcqttjxse07cv7",
            "addr1q8ya8tfyfd9ptgqd3k932qchunqs8tet7wshsdwwnk9dh2nh0jh52gfgfyg24da8jkjrfevncl3wsu0dfurs2jq5e95st0w5yc",
            "addr1q8uqehrrwuccm8dncjp5mth7c9743vr6ewpy07nvcusr5tdd7w756gtn2da2d54rwcwgv6xurv4zllv5f58r8w8t9m3sdgmdzk",
            "addr1q8clzj8gxlwz9ae7f28dgkyle0p07qzxjaern33wqd6vap83skegcg222ukkjcdhyuxq8yc95kxkwekt3dv5gkdcw33suz2psg",
            "addr1qypgyk8zxkj4tj96m48cxrke4gptv7hcg8l4x0j0l2e3jkx5ff5dxynhm5gp2hqphan9z8nef8c66qqj5gapxutcnwks5wggsx",
            "addr1qy3df238yav8a60tj6azrmty0ur3v0atdvzem7zu9grjvdleamndlj0g6nuzsdjss3wl9yd0ddm877gz76wmm5w4k2wqxqrmmy",
            "addr1qxdgdzh752p7wdzekqhr3xmzkcj0s0m8alcgvugl8hcwl4um37738jmtjmq5jhz6wcqjjlttc5cunykv5xdd2pqddpcqeyltvn",
            "addr1q8pnq5m7ap7s4d8udaglg3llx6r9stmwl8v83fryfs2kwj4z2s3pvww78kp2curplsgxrkcggz0addvg6c3ewqxtlcksnuegwx",
            "addr1qy8rt8m57whncvkgr3n4p5ya6vjn58rxx64f25a8sa694ksw6rjlmvmu4pmrxu4a5zmh8k5sc9vh4gt43vudadky858qm6trrx",
            "addr1q87cryd0mh2ea5nnmsxjlh2eslle2a5squ485h9hh35m2jrrqzwpp7pxyu7vayu0g6nvlan4v0tecw5t3u2fqqs3n5rs49vsd8",
            "addr1qxkf9h4l235vgmcqva9ktkuj6zjp060r8c6r20a4nyrr26dn0pfpp0zadd34h6wpd9fw8rpl6d0a9mxrplkzet569vgskhwsvr",
            "addr1q8ewc68l7ru25za2ukh4v640k9nkqtu4u0s9qcq77qk0xz7drrhs3238hz7wxc3yujrtxtngnnte4vdgxqjgfxp5jcgqr8cdx5",
            "addr1q9ws03prkeg69exgeqey83qvhc8ff2etzt48xssyk0z7ury7jwxf9cg7u98e4kp2w8e7mt9czw6s2tmj875vujdurjvs0llgcf",
            "addr1qxjqhfvgm47q6kwflu9ged6jk35m0xy3d0kpjesmx65jlz750n0mrz6hqe90qhwtxevv4gxg5npsf2ysjl4r86f8e08sqp56vk",
            "addr1qxygyd95ezwufthzaqc8c8unfaqnq2xn85n6d2ds3l2cvmk5lv67uxusxae5rls2e5xnk062cgmw732rggd6nlv84clqcuxl69",
            "addr1qxaz4ccsex7h9ww5jdyq36hvta9w8ymf9620mf2pzd07kmj7j94qfd00kqpstrcmuygpcggc83vufrtvymgahstswpjqjf2v5u",
            "addr1q80rrxsl8a3lv3849x6j8fkufnl5s5uwtqpqk80680e97rynur3f0l8whgga89dfkmw2ap2lcdql6xdnchxqrnyt3elsfkplmc",
            "addr1q852tp00me3a6rx959rp284r33vk9u09dv73w6aq2kz7xv04960qj00x3uh9ztgpqn56yw2uzgwkxfymujc03e9epyfqf2qrnd",
            "addr1qy5j4aedrmk5jdqhn9zsp87l0v6r5y6q3854vkh0makv9nxje3qt8rv85hlytu56vedsraqd8gyxu8xhcn79j57pnqdstxal0v",
            "addr1qyy65kruwwqx3vvewzn7kr6n32xulp4kv62hqdyj2wcdlwz2euat7gefkymjzsdt2g06kfhdzu859fq39s5xnuuetp9saelrds",
            "addr1qyp5gsptw06pp007lnp26ua4qwy67f6t9y5k0gg425s3ujzwsspa2avgzrm5fqml9lfxr60pfzr3fvkss44fvsyj4tpsg060e4",
            "addr1q86y6xp7zs9qtyqlvukjr72un0yrlqe6azljmu4xtygqpj85h640ht3dqrzt6y9a2dq6c6ys00rhu5ames0a5e0v0sss0pnq6u",
            "addr1q84vd9lhf8ld2e5mf38tudp2xledtw8l6mu8ahu20t6a0ae7ar4pux6z89y97rc8mmgaqzzhz768g8f5cc3puc2ng6zqsv8t82",
            "addr1q9dx6hmu7hatsmeegkulxcjtrza30rjnnzuv6xflmj7fwjuz07jx35l7mnhvtpt4aj8s9gkte5uv68w8jt000x96dmus7k8avl",
            "addr1qxgecj367l24apqgkdmzl3vgpkyezne8ecdl8gtcysgldadhg4epz677aj2m98vsuzg3dyd3tjsxme0ujupsp95v5k7qvx6uvk",
            "addr1q9ekg3rg7gq8y2upucfuzvn3a0ygexa0ccv4u7379n674sa3fpwwzwg06zmvc9dv7ktzw8jtvnl7u735gumuzmatw96q77r99y",
            "addr1qxzlyzx9dlz42lzp8hk92qhanxnm4jwk67klh4h5qxvyexjgfwfvjdl622awer8lzhlx936m0gmj3sas7trpxpq9jhgsnr3nw4",
            "addr1q84flut7apsxycj5wvphlkx7dllc3alafv2fgwnuevxxev3pr506n8l3r8mjvjke3vlsscf40mdu5aqcx4e4z5u4754slqyn47",
            "addr1qxnrnmlu488kl27sh596zhh7jr7uk8cpnzkd03evyxaner42wvmmhf6wdm9u4lp6gx2r73tlqn42535wx6sr8l6yv3jqzn5xaf",
            "addr1qyctlwdsgfdg69yznrwcx3tp62y065m49w36vc5tzallrgydlj4rggqyvsj2k2l8dwjfpe0ms6tpkcz4pa7z4nq5mp7q5ktaw4",
        )

        addresses.forEach { address ->
            val stakeAddress = address.extractStakeAddress(true)
            println(stakeAddress)
        }
    }
}
