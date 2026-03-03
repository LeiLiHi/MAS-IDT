package org.lileischeduler.dynamicGP.LLMChat;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.time.Duration;

public interface AiAssistant {
    @UserMessage("""
            Don't output ```json in line one and ``` at the end
            
            There are 9 attributes and their meaning.
            "DownloadNum":The number of data transmission windows, the higher，the better
            "DownloadDuration":The duration of data transmission windows, the higher，the better
            "HistoryScore":The score calculated based on historical data, the higher，the better
            "DownloadRate":The time required to download the data generated for each second of imaging, the higher，the worse
            "Storage":The maximum storage, the higher, the better
            "Energy":The maximum energy, the higher, the better
            "Transition":The satellite's attitude adjustment capability, the higher, the worse
            "Inclination":The orbital inclination of a satellite
            "EnergyConsumption":The energy consumption of a satellite in performing its mission, the higher, the worse
            
            Generate a new expression based on the provided expression and their performance of elite individuals.
            {{info}}
            
            
            The output format must be consistent with the following content,
            
            Expression: min(((max(((Transition - (Transition - DownloadNum)) + EnergyConsumption), DownloadDuration) - ((((DownloadDuration - HistoryScore) * (Transition + DownloadRate)) - (Inclination - HistoryScore)) + min((Storage + DownloadRate), Inclination))) * min(min(HistoryScore, Inclination), Storage)), (min(max(Inclination, (DownloadRate + max((Inclination + Storage), Transition))), (HistoryScore * HistoryScore)) + DownloadDuration))
            Descriptions: Explain the meaning of generated expression.
            """)
    String generateIndividual(@V("info") String info);


    @UserMessage("""
            Don't output ```json in line one and ``` at the end
            
            There are 9 attributes and their meaning.
            "DownloadNum":The number of data transmission windows, the higher，the better
            "DownloadDuration":The duration of data transmission windows, the higher，the better
            "HistoryScore":The score calculated based on historical data, the higher，the better
            "DownloadRate":The time required to download the data generated for each second of imaging, the higher，the worse
            "Storage":The maximum storage, the higher, the better
            "Energy":The maximum energy, the higher, the better
            "Transition":The satellite's attitude adjustment capability, the higher, the worse
            "Inclination":The orbital inclination of a satellite
            "EnergyConsumption":The energy consumption of a satellite in performing its mission, the higher, the worse
            
            Generate a double[] based on the information provided below. Its length should be equal to the number of attributes mentioned above, and the numbers inside represent the probability of each attribute being selected, with the minimum value being 0.05.
            The sum result of these probabilities should be 1.
            The output format must be consistent with the following content,
            
            Probabilities: [0.2,0.3,...]
            Descriptions: Explain the probabilities.
            
            The information are bellow:
            
            {{info}}
            """)
    String summarizeAsArray(@V("info") String info);
}
