#!/usr/bin/env node

const ying = "----      ----"
const yang = "---- ---- ----"

const randPrint = () => {
    console.log(Math.random() > 0.5 ? ying : yang)
}

for(let i=0; i< 6;i++) {
    randPrint()
}
