#!/usr/bin/env bash

apt-get update
apt-get install zsh git curl wget file -y

# see https://gitmirror.com/raw.html
# git clone https://gitclone.com/github.com
# git clone https://ghproxy.com/https://github.com
sh -c "$(curl -fsSL https://raw.gitmirror.com/ohmyzsh/ohmyzsh/master/tools/install.sh | sed 's/github/kgithub/g')"

# themes
cp ~/.oh-my-zsh/themes/agnoster.zsh-theme ~/.oh-my-zsh/custom/themes/myagnoster.zsh-theme
sed -i 's/blue/cyan/' ~/.oh-my-zsh/custom/themes/myagnoster.zsh-theme
sed -i 's/^ZSH_THEME=".*"/ZSH_THEME="myagnoster"/' ~/.zshrc

# plugins
cd ~/.oh-my-zsh/custom/plugins || return 1
git clone https://kgithub.com/zsh-users/zsh-autosuggestions --depth 1
git clone https://kgithub.com/zsh-users/zsh-syntax-highlighting --depth 1
sed -i 's/plugins=(git)/plugins=(git zsh-autosuggestions zsh-syntax-highlighting)/' ~/.zshrc
sed -i '$a source ~/.oh-my-zsh/custom/plugins/zsh-syntax-highlighting/zsh-syntax-highlighting.zsh' ~/.zshrc

cat <<EOF >> ~/.zshrc

alias dk="sudo docker"
alias dki="dk image"
alias dke="dk exec -it"
alias dkr="dk run --rm=true"
alias dkl="dk logs -f"
alias dkv="dk volume"
alias dkn="dk network"
alias dkc="dk container"
alias dkic="dk inspect container"
alias dkii="dk inspect image"
alias dkm="dk machine"
alias docker-compose="podman-compose"
alias dkp="docker-compose"
alias dkpe="docker-compose exec"
EOF
