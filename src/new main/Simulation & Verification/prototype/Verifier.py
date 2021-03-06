import random
from datetime import datetime
random.seed(datetime.now())


class Verifier(object):
    def __init__(self, checker):
        self.propertyChecker = checker

    def verify(self, simulationLogs, verificationProperty):
        check = True
        for simulationLog in simulationLogs:
            if not self.propertyChecker.check(simulationLog, property):
                check = False
        return check


class SPRT(Verifier):
    def __init__(self, checker):
        super(SPRT, self).__init__(checker)
        self.alpha = 0.05
        self.beta = 0.05
        self.delta = 0.01
        self.minimumSample = 2

    def verifyExistedLogs(self, simulationLogs, verificationProperty):
        for i in range(1, 100):
            theta = i * 0.01
            numOfSamples = 0
            numOfTrue = 0
            random.shuffle(simulationLogs)

            while self.isSampleNeeded(numOfSamples, numOfTrue, theta):
                logLen = len(simulationLogs)
                if not numOfSamples < logLen:
                    print("Lack of simulation logs:", logLen)
                    break

                if self.propertyChecker.check(simulationLogs[numOfSamples], verificationProperty):
                    numOfTrue = numOfTrue + 1
                numOfSamples = numOfSamples + 1

            result = self.isSatisfied(numOfSamples, numOfTrue, theta)   #todo: 각 theta에 대한 결정 함수 필요, 여기서 alpha, beta, delta 사용
            print('theta:', format(theta, ".2f"), ' num of samples:', numOfSamples, ' num of true:', numOfTrue, ' result:', result)

    def verifyWithSimulator(self, simulator, verificationProperty, maxRepeat):
        maxNumOfSamples = maxRepeat

        for i in range(1, 100):
            theta = i * 0.01
            numOfSamples = 0
            numOfTrue = 0

            while self.isSampleNeeded(numOfSamples, numOfTrue, theta):
                if not numOfSamples < maxNumOfSamples:
                    print("Over maximum repeat:", maxNumOfSamples)
                    break
                simulationLog = simulator.run()

                if self.propertyChecker.check(simulationLog, verificationProperty):
                    numOfTrue = numOfTrue + 1
                numOfSamples = numOfSamples + 1

            result = self.isSatisfied(numOfSamples, numOfTrue, theta)   #todo: 각 theta에 대한 결정 함수 필요, 여기서 alpha, beta, delta 사용
            print('theta:', format(theta, ".2f"), ' num of samples:', numOfSamples, ' num of true:', numOfTrue, ' result:', result)

    def isSampleNeeded(self, numOfSamples, numOfTrue, theta):   #todo: 샘플 필요한지
        if numOfSamples < self.minimumSample:
            return True

        h0Threshold = self.beta/(1-self.alpha)
        h1Threshold = (1-self.beta)/self.alpha

        v = self.getV(numOfSamples, numOfTrue, theta)

        if v <= h0Threshold:
            return False
        elif v >= h1Threshold:
            return False
        else:
            return True

    def isSatisfied(self, numOfSamples, numOfTrue, theta):
        h0Threshold = self.beta / (1 - self.alpha)

        v = self.getV(numOfSamples, numOfTrue, theta)

        if v <= h0Threshold:
            return True
        else:
            return False

    def getV(self, numOfSamples, numOfTrue, theta): #todo
        p0 = theta + self.delta
        p1 = theta - self.delta

        numOfFalse = numOfSamples - numOfTrue
        p1m = ((p1 ** numOfTrue) * ((1 - p1) ** numOfFalse))
        p0m = ((p0 ** numOfTrue) * ((1 - p0) ** numOfFalse))
        if p0m == 0:
            p1m = p1m + 0.000000001
            p0m = p0m + 0.000000001
        v = p1m / p0m

        return v
