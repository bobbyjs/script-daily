#!/usr/bin/env node

const ying = "------------            ------------"
const yang = "------------------------------------"

const randi = (min, max) => {
    return Math.floor(Math.random() * (max - min) + min);
}

const randPrint = (chosen) => {
    if (chosen) {
        console.log('\x1B[32m%s\x1B[0m', Math.random() > 0.5 ? ying : yang)
    } else {
        console.log(Math.random() > 0.5 ? ying : yang)
    }
}

const index = randi(0, 6)
for (let i = 0; i < 6; i++) {
    randPrint(index === i)
    console.log()
}
