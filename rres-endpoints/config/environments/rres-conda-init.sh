# Initialises Conda/Snakemake for the RRes environment
#
# ===> This NEEDS TO BE RUN MANUALLY BEFORE scripts that use Snakemake
#

# This is what conda installation puts in .profile
__conda_setup="$('/home/data/knetminer/software/conda/mamba/bin/conda' 'shell.bash' 'hook' 2> /dev/null)"
if [ $? -eq 0 ]; then
    eval "$__conda_setup"
else
    if [ -f "/home/data/knetminer/software/conda/mamba/etc/profile.d/conda.sh" ]; then
        . "/home/data/knetminer/software/conda/mamba/etc/profile.d/conda.sh"
    else
        export PATH="/home/data/knetminer/software/conda/mamba/bin:$PATH"
    fi
fi
unset __conda_setup
# End of conda installation snippet

conda activate snakemake
