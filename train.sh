#!/usr/bin/bash

#SBATCH -J test-run
#SBATCH --gres=gpu:1
#SBATCH --cpus-per-gpu=8
#SBATCH --mem-per-gpu=29G
#SBATCH -p batch_ce_ugrad
#SBATCH -w moana-y3
#SBATCH -t 1-0
#SBATCH -o logs/slurm-%A.out

pwd
which python
hostname
python yolo_train.py

exit 0
