import subprocess
from ultralytics import YOLO
from roboflow import Roboflow

subprocess.run('yolo', 'task=detect', 'mode=train', 'model=yolov8s.pt', 'data=../../../../local_datasets/FOOD-INGREDIENTS-dataset-4/data.yaml', 'plots=True')